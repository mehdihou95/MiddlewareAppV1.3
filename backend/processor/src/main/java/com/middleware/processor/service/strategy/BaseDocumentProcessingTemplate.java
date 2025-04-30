package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.processor.service.util.TransformationService;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstract template for document processing strategies.
 * Provides common functionality for validation, transformation, and persistence.
 * Standardized transaction management with REQUIRED propagation.
 */
public abstract class BaseDocumentProcessingTemplate extends BaseDocumentProcessingStrategy {

    private static final Logger log = LoggerFactory.getLogger(BaseDocumentProcessingTemplate.class);

    @Autowired
    protected XmlValidationService xmlValidationService;

    @Autowired
    protected MappingRuleRepository mappingRuleRepository;

    @Autowired
    protected ProcessedFileRepository processedFileRepository;

    @Autowired
    protected TransformationService transformationService;

    @Autowired
    protected XmlProcessor xmlProcessor;

    @Autowired
    protected CircuitBreakerService circuitBreakerService;

    /**
     * Main processing method that orchestrates the document processing flow.
     * Runs in a new transaction to ensure atomicity for each document.
     *
     * @param file            The multipart file containing the XML document.
     * @param interfaceEntity The interface configuration for this document.
     * @return The ProcessedFile entity representing the result.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        log.debug("Processing document {} for interface {}", file.getOriginalFilename(), interfaceEntity.getName());

        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setFileName(file.getOriginalFilename());
        processedFile.setStatus("PROCESSING");
        processedFile.setInterfaceEntity(interfaceEntity);
        processedFile.setClient(interfaceEntity.getClient());
        processedFile.setProcessedAt(LocalDateTime.now());
        
        try {
            // Save initial processing status
            processedFile = processedFileRepository.save(processedFile);

            // Parse XML document
            Document document = xmlProcessor.parseXmlFile(file);

            // Validate XML
            validateXml(document, interfaceEntity);

            // Process document
            Object processedEntity = processSpecificDocument(document, interfaceEntity);

            // Update processed file status
            processedFile.setStatus("SUCCESS");
            processedFile.setContent(xmlProcessor.serializeDocument(document));
            return processedFileRepository.save(processedFile);

        } catch (ValidationException e) {
            log.error("Validation error processing document {}: {}", file.getOriginalFilename(), e.getMessage());
            processedFile.setStatus("ERROR");
            processedFile.setErrorMessage("Validation error: " + e.getMessage());
            return processedFileRepository.save(processedFile);
        } catch (Exception e) {
            log.error("Error processing document {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            processedFile.setStatus("ERROR");
            processedFile.setErrorMessage("Processing error: " + e.getMessage());
            return processedFileRepository.save(processedFile);
        }
    }

    /**
     * Validates an XML document against its schema and performs additional validations.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @throws ValidationException If validation fails.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected void validateXml(Document document, Interface interfaceEntity) throws ValidationException {
        if (interfaceEntity.getSchemaPath() != null && !interfaceEntity.getSchemaPath().isEmpty()) {
            boolean isValid = xmlValidationService.validateXmlContent(document, interfaceEntity);
            if (!isValid) {
                throw new ValidationException("XML validation failed against schema: " + interfaceEntity.getSchemaPath() +
                        ". Error: " + xmlValidationService.getValidationErrorMessage());
            }
        } else {
            log.warn("No XSD schema path defined for interface {}, skipping schema validation.", interfaceEntity.getName());
        }
        // Add further structural or business rule validations if needed
        performAdditionalValidations(document, interfaceEntity);
    }

    /**
     * Placeholder for additional validation logic specific to the document type.
     * To be implemented by subclasses if needed.
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @throws ValidationException If additional validation fails.
     */
    protected void performAdditionalValidations(Document document, Interface interfaceEntity) throws ValidationException {
        // Default implementation does nothing
    }

    /**
     * Abstract method for specific document processing logic.
     * Must be implemented by concrete strategy classes.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @return The processed domain entity (e.g., AsnHeader, OrderHeader).
     * @throws Exception If processing fails.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected abstract Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception;

    /**
     * Retrieves active mapping rules for a given interface.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param interfaceId The ID of the interface.
     * @return A list of active mapping rules.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    protected List<MappingRule> getActiveMappingRules(Long interfaceId) {
        return mappingRuleRepository.findByInterfaceIdAndIsActiveTrue(interfaceId);
    }

    /**
     * Applies a transformation rule to a given value.
     *
     * @param value The original value.
     * @param rule  The mapping rule containing transformation details.
     * @return The transformed value.
     */
    protected String applyTransformation(String value, MappingRule rule) {
        if (rule.getTransformation() != null && !rule.getTransformation().isEmpty()) {
            try {
                return (String) transformationService.transformAndConvert(value, rule.getTransformation(), String.class);
            } catch (Exception e) {
                log.error("Transformation failed for rule {} and value '{}': {}", rule.getName(), value, e.getMessage());
                // Decide how to handle transformation errors (e.g., use default, skip, throw exception)
                return rule.getDefaultValue() != null ? rule.getDefaultValue() : value; // Example: fallback to default or original
            }
        }
        return value;
    }
}

