package com.middleware.processor.service.strategy;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.interfaces.DocumentProcessingStrategyService;
import com.middleware.processor.service.interfaces.ProcessedFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Base class for document processing strategies.
 * Implements common functionality and provides default implementations.
 */
@Service
public abstract class BaseDocumentProcessingStrategy implements DocumentProcessingStrategyService {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseDocumentProcessingStrategy.class);
    
    @Autowired
    protected ProcessedFileService processedFileService;
    
    @Override
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        try {
            ProcessedFile processedFile = createInitialProcessedFile(interfaceEntity);
            return updateProcessedFile(processedFile, "SUCCESS", "Document processed successfully");
        } catch (Exception e) {
            logger.error("Error processing document: {}", e.getMessage(), e);
            ProcessedFile errorFile = createInitialProcessedFile(interfaceEntity);
            return updateProcessedFile(errorFile, "ERROR", e.getMessage());
        }
    }
    
    @Override
    public BaseDocumentProcessingStrategy getStrategy(String interfaceType) {
        return this;
    }
    
    /**
     * Create an initial processed file record.
     */
    protected ProcessedFile createInitialProcessedFile(Interface interfaceEntity) {
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setInterfaceEntity(interfaceEntity);
        processedFile.setStatus("PENDING");
        processedFile.setCreatedAt(LocalDateTime.now());
        return processedFile;
    }
    
    /**
     * Update a processed file with new status and message.
     */
    protected ProcessedFile updateProcessedFile(ProcessedFile processedFile, String status, String message) {
        processedFile.setStatus(status);
        if ("ERROR".equals(status)) {
            processedFile.setErrorMessage(message);
        } else {
            processedFile.setContent(message);
        }
        return processedFileService.createProcessedFile(processedFile);
    }
    
    /**
     * Get the priority of this strategy.
     * Higher numbers indicate higher priority.
     * Default implementation returns 0.
     */
    @Override
    public int getPriority() {
        return 0;
    }
    
    /**
     * Get the document type this strategy handles.
     * @return The document type (e.g., "ASN", "ORDER", "XML")
     */
    public abstract String getDocumentType();
    
    /**
     * Check if this strategy can handle the given document type.
     * @param documentType The document type to check
     * @return true if this strategy can handle the document type, false otherwise
     */
    public abstract boolean canHandle(String documentType);
    
    /**
     * Get the name of this strategy.
     * @return The strategy name
     */
    public abstract String getName();
}
