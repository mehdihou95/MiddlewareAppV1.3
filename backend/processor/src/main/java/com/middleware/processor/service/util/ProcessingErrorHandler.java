package com.middleware.processor.service.util;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.interfaces.ProcessedFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Centralized error handling for document processing.
 * Standardizes error handling across different processing strategies.
 */
@Component
public class ProcessingErrorHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ProcessingErrorHandler.class);
    
    private final ProcessedFileService processedFileService;
    
    @Autowired
    public ProcessingErrorHandler(ProcessedFileService processedFileService) {
        this.processedFileService = processedFileService;
    }
    
    /**
     * Handle processing errors by updating or creating error records
     * 
     * @param e The exception that occurred
     * @param fileName The name of the file being processed
     * @param interfaceEntity The interface entity associated with the file
     * @param existingFile An existing ProcessedFile record, if available
     * @return The updated or newly created ProcessedFile with error status
     */
    public ProcessedFile handleProcessingError(Exception e, String fileName, Interface interfaceEntity, ProcessedFile existingFile) {
        log.error("Processing error: ", e);
        
        String errorMessage = createErrorMessage("Processing error", e);
        
        if (existingFile != null && existingFile.getId() != null) {
            log.debug("Updating existing processed file record with error status");
            existingFile.setStatus("ERROR");
            existingFile.setErrorMessage(errorMessage);
            return processedFileService.updateProcessedFile(existingFile.getId(), existingFile);
        } else {
            log.debug("Creating new processed file record with error status");
            ProcessedFile errorFile = new ProcessedFile();
            errorFile.setFileName(fileName);
            errorFile.setStatus("ERROR");
            errorFile.setInterfaceEntity(interfaceEntity);
            errorFile.setClient(interfaceEntity.getClient());
            errorFile.setProcessedAt(LocalDateTime.now());
            errorFile.setErrorMessage(errorMessage);
            return processedFileService.createProcessedFile(errorFile);
        }
    }
    
    /**
     * Create a standardized error message with truncation
     * 
     * @param prefix The prefix for the error message
     * @param e The exception containing the error message
     * @return A formatted error message, truncated if necessary
     */
    public String createErrorMessage(String prefix, Exception e) {
        String errorMessage = prefix + ": " + e.getMessage();
        if (errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 997) + "...";
        }
        return errorMessage;
    }
    
    /**
     * Handle validation errors
     * 
     * @param errorMessage The validation error message
     * @param fileName The name of the file being validated
     * @param interfaceEntity The interface entity associated with the file
     * @param existingFile An existing ProcessedFile record, if available
     * @return The updated or newly created ProcessedFile with error status
     */
    public ProcessedFile handleValidationError(String errorMessage, String fileName, Interface interfaceEntity, ProcessedFile existingFile) {
        log.error("Validation error: {}", errorMessage);
        
        if (errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 997) + "...";
        }
        
        if (existingFile != null && existingFile.getId() != null) {
            existingFile.setStatus("ERROR");
            existingFile.setErrorMessage(errorMessage);
            return processedFileService.updateProcessedFile(existingFile.getId(), existingFile);
        } else {
            ProcessedFile errorFile = new ProcessedFile();
            errorFile.setFileName(fileName);
            errorFile.setStatus("ERROR");
            errorFile.setInterfaceEntity(interfaceEntity);
            errorFile.setClient(interfaceEntity.getClient());
            errorFile.setProcessedAt(LocalDateTime.now());
            errorFile.setErrorMessage(errorMessage);
            return processedFileService.createProcessedFile(errorFile);
        }
    }
}
