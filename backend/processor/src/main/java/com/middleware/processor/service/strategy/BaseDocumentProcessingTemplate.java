package com.middleware.processor.service.strategy;

import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.interfaces.MappingRuleService;
import com.middleware.processor.service.interfaces.ProcessedFileService;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.processor.exception.MappingRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base template for document processing strategies.
 * Provides common functionality and transaction management.
 */
public abstract class BaseDocumentProcessingTemplate extends BaseDocumentProcessingStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseDocumentProcessingTemplate.class);
    
    protected static final String STATUS_PROCESSING = "PROCESSING";
    protected static final String STATUS_COMPLETED = "COMPLETED";
    protected static final String STATUS_FAILED = "FAILED";
    protected static final String STATUS_ERROR = "ERROR";
    
    @Autowired
    protected MappingRuleService mappingRuleService;
    
    @Autowired
    protected ProcessedFileService processedFileService;
    
    @Autowired
    protected XmlProcessor xmlProcessor;
    
    @Autowired
    protected CircuitBreakerService circuitBreakerService;
    
    /**
     * Process a document according to the strategy.
     * This method uses REQUIRED propagation to ensure transaction consistency.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        ProcessedFile processedFile = null;
        try {
            logger.info("Starting document processing for file: {}", file.getOriginalFilename());
            
            // Create processed file first
            processedFile = new ProcessedFile();
            processedFile.setFileName(file.getOriginalFilename());
            processedFile.setInterfaceEntity(interfaceEntity);
            processedFile.setClient(interfaceEntity.getClient());
            processedFile.setProcessedAt(LocalDateTime.now());
            processedFile.setStatus(STATUS_PROCESSING);
            processedFile = processedFileService.createProcessedFile(processedFile);
            logger.info("Created processed file with id: {}", processedFile.getId());

            // Parse document
            Document document = xmlProcessor.parseXmlFile(file);
            logger.info("Successfully parsed XML document");
            
            // Process document
            processParsedDocument(document, interfaceEntity, processedFile);
            
            // Update status to completed
            processedFile.setStatus(STATUS_COMPLETED);
            return processedFileService.updateProcessedFile(processedFile.getId(), processedFile);
            
        } catch (Exception e) {
            logger.error("Error processing document: {}", e.getMessage(), e);
            if (processedFile != null) {
                try {
                    processedFile.setStatus(STATUS_FAILED);
                    processedFile.setErrorMessage(e.getMessage());
                    processedFileService.updateProcessedFile(processedFile.getId(), processedFile);
                } catch (Exception ex) {
                    logger.error("Failed to update processed file status: {}", ex.getMessage(), ex);
                }
            }
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a parsed document according to the strategy.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    protected ProcessedFile processParsedDocument(Document document, Interface interfaceEntity, ProcessedFile processedFile) {
        try {
            logger.info("Starting parsed document processing for file: {}", processedFile.getFileName());
            
            // Get mapping rules with proper error handling
            List<MappingRule> headerRules;
            List<MappingRule> lineRules;
            try {
                headerRules = getMappingRules(interfaceEntity, getHeaderTableName());
                lineRules = getMappingRules(interfaceEntity, getLineTableName());
                logger.info("Retrieved {} header rules and {} line rules", headerRules.size(), lineRules.size());
            } catch (MappingRuleException e) {
                String error = String.format("Failed to fetch mapping rules: %s (Table: %s, Client: %s)", 
                    e.getMessage(), e.getTableName(), e.getClientId());
                logger.error(error);
                return updateProcessedFileStatus(processedFile, STATUS_ERROR, error);
            }
            
            // Process header
            Object header = createDefaultHeader(interfaceEntity.getClient());
            AtomicBoolean hasErrors = new AtomicBoolean(false);
            StringBuilder errorMessages = new StringBuilder();
            
            logger.info("Processing header");
            if (!processHeader(header, headerRules, document, hasErrors, errorMessages)) {
                String error = "Header processing failed: " + errorMessages.toString();
                logger.error(error);
                return updateProcessedFileStatus(processedFile, STATUS_ERROR, error);
            }
            
            // Save header
            logger.info("Saving header");
            Object savedHeader = saveHeader(header, hasErrors, errorMessages);
            if (hasErrors.get()) {
                String error = "Header save failed: " + errorMessages.toString();
                logger.error(error);
                return updateProcessedFileStatus(processedFile, STATUS_ERROR, error);
            }
            
            // Process lines
            logger.info("Processing lines");
            if (!processLines(savedHeader, lineRules, document, interfaceEntity, hasErrors, errorMessages)) {
                String error = "Line processing failed: " + errorMessages.toString();
                logger.error(error);
                return updateProcessedFileStatus(processedFile, STATUS_ERROR, error);
            }
            
            logger.info("Document processing completed successfully");
            return updateProcessedFileStatus(processedFile, STATUS_COMPLETED, "Document processed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing document: {}", e.getMessage(), e);
            return updateProcessedFileStatus(processedFile, STATUS_ERROR, "Processing error: " + e.getMessage());
        }
    }
    
    /**
     * Get mapping rules for an interface and table.
     * @throws MappingRuleException if rules cannot be fetched or are invalid
     */
    protected List<MappingRule> getMappingRules(Interface interfaceEntity, String tableName) {
        if (interfaceEntity == null || interfaceEntity.getClient() == null) {
            throw new MappingRuleException(
                "Invalid interface or client data",
                tableName,
                interfaceEntity != null ? interfaceEntity.getClient().getId() : null
            );
        }

        try {
            List<MappingRule> rules = circuitBreakerService.<Page<MappingRule>>executeRepositoryOperation(
                () -> mappingRuleService.findByClientIdAndInterfaceIdAndTableName(
                    interfaceEntity.getClient().getId(),
                    interfaceEntity.getId(),
                    tableName,
                    Pageable.unpaged()
                ),
                () -> {
                    logger.warn("Circuit breaker open while fetching mapping rules for table: {}", tableName);
                    return Page.empty();
                }
            ).getContent();

            if (rules.isEmpty()) {
                throw new MappingRuleException(
                    String.format("No mapping rules found for table %s, client %d, and interface %d", 
                        tableName, 
                        interfaceEntity.getClient().getId(),
                        interfaceEntity.getId()),
                    tableName,
                    interfaceEntity.getClient().getId()
                );
            }

            // Validate that all required rules are present and active
            validateMappingRules(rules, tableName);

            return rules;
        } catch (MappingRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingRuleException(
                String.format("Error fetching mapping rules: %s", e.getMessage()),
                tableName,
                interfaceEntity.getClient().getId(),
                e
            );
        }
    }
    
    /**
     * Validate mapping rules for completeness and correctness
     */
    private void validateMappingRules(List<MappingRule> rules, String tableName) {
        if (rules.stream().noneMatch(MappingRule::getIsActive)) {
            throw new MappingRuleException(
                String.format("No active mapping rules found for table %s", tableName),
                tableName,
                rules.isEmpty() ? null : rules.get(0).getClient().getId()
            );
        }

        // Add any additional validation rules here
        // For example, checking for required fields or valid XML paths
    }
    
    /**
     * Create an initial processed file record
     */
    protected ProcessedFile createInitialProcessedFile(Interface interfaceEntity) {
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setInterfaceEntity(interfaceEntity);
        processedFile.setClient(interfaceEntity.getClient());
        processedFile.setProcessedAt(LocalDateTime.now());
        return processedFile;
    }
    
    /**
     * Update the status of a processed file.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    protected ProcessedFile updateProcessedFileStatus(ProcessedFile processedFile, String status, String message) {
        try {
            processedFile.setStatus(status);
            if (STATUS_ERROR.equals(status)) {
                processedFile.setErrorMessage(message);
            } else {
                processedFile.setContent(message);
            }
            
            return circuitBreakerService.executeRepositoryOperation(
                () -> processedFileService.updateProcessedFile(processedFile.getId(), processedFile),
                () -> processedFile
            );
        } catch (Exception e) {
            logger.error("Error updating processed file status: {}", e.getMessage(), e);
            return processedFile;
        }
    }
    
    /**
     * Get the document type for this strategy
     */
    public abstract String getDocumentType();
    
    /**
     * Get the name of the header table.
     */
    protected abstract String getHeaderTableName();
    
    /**
     * Get the name of the line table.
     */
    protected abstract String getLineTableName();
    
    /**
     * Create a default header object.
     */
    protected abstract Object createDefaultHeader(Client client);
    
    /**
     * Process the header of a document.
     */
    protected abstract boolean processHeader(Object header, List<MappingRule> rules, Document document, 
            AtomicBoolean hasErrors, StringBuilder errorMessages);
    
    /**
     * Save a header object.
     */
    protected abstract Object saveHeader(Object header, AtomicBoolean hasErrors, StringBuilder errorMessages);
    
    /**
     * Process the lines of a document.
     */
    protected abstract boolean processLines(Object savedHeader, List<MappingRule> rules, Document document,
            Interface interfaceEntity, AtomicBoolean hasErrors, StringBuilder errorMessages);
}
