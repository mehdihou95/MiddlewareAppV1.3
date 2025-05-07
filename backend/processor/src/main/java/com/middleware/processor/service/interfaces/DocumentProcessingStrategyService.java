package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.strategy.BaseDocumentProcessingStrategy;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for document processing strategy management.
 * Provides methods for processing documents and retrieving strategies.
 */
public interface DocumentProcessingStrategyService {
    
    /**
     * Process a document using the appropriate strategy
     * 
     * @param file The file to process
     * @param interfaceEntity The interface entity
     * @return The processed file
     */
    ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity);
    
    /**
     * Get the appropriate strategy for the specified interface type
     * 
     * @param interfaceType The interface type
     * @return The appropriate strategy
     */
    BaseDocumentProcessingStrategy getStrategy(String interfaceType);
    
    /**
     * Get the priority of this strategy.
     * Higher numbers indicate higher priority.
     * 
     * @return The priority of this strategy
     */
    int getPriority();
}
