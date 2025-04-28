package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.interfaces.DocumentProcessingStrategyService;
import com.middleware.processor.service.strategy.BaseDocumentProcessingStrategy;
import com.middleware.processor.service.strategy.DocumentProcessingStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.annotation.Primary;

/**
 * Implementation of DocumentProcessingStrategyService.
 * Uses the strategy factory to route documents to the appropriate processing strategy.
 */
@Service
@Primary
public class DocumentProcessingStrategyServiceImpl implements DocumentProcessingStrategyService {

    private final DocumentProcessingStrategyFactory strategyFactory;
    
    @Autowired
    public DocumentProcessingStrategyServiceImpl(DocumentProcessingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Override
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        BaseDocumentProcessingStrategy strategy = getStrategy(interfaceEntity.getType());
        if (strategy == null) {
            throw new ValidationException("No processing strategy found for interface type: " + interfaceEntity.getType());
        }
        return strategy.processDocument(file, interfaceEntity);
    }

    @Override
    public BaseDocumentProcessingStrategy getStrategy(String interfaceType) {
        return strategyFactory.getStrategy(interfaceType);
    }

    @Override
    public int getPriority() {
        return 0; // Default priority for the service implementation
    }
}
