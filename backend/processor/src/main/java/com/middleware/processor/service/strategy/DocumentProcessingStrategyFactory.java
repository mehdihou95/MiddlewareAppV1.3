package com.middleware.processor.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for document processing strategies.
 * Manages strategy instances and provides appropriate strategy based on document type.
 */
@Component
public class DocumentProcessingStrategyFactory {
    private final Map<String, BaseDocumentProcessingStrategy> strategyMap = new ConcurrentHashMap<>();
    private final BaseDocumentProcessingStrategy defaultStrategy;
    
    /**
     * Constructor that automatically registers all available strategies
     * 
     * @param strategies List of all BaseDocumentProcessingStrategy beans
     */
    @Autowired
    public DocumentProcessingStrategyFactory(List<BaseDocumentProcessingStrategy> strategies) {
        strategies.forEach(strategy -> strategyMap.put(strategy.getDocumentType().toUpperCase(), strategy));
        
        // Set ASN as the default strategy or throw an exception if none is found
        defaultStrategy = strategies.stream()
            .filter(s -> "ASN".equalsIgnoreCase(s.getDocumentType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No default strategy found"));
    }
    
    /**
     * Get the appropriate strategy for the specified interface type
     * 
     * @param interfaceType The interface type
     * @return The appropriate strategy, or the default strategy if no specific one is found
     */
    public BaseDocumentProcessingStrategy getStrategy(String interfaceType) {
        if (interfaceType == null) {
            return defaultStrategy;
        }
        
        return strategyMap.getOrDefault(interfaceType.toUpperCase(), defaultStrategy);
    }
    
    /**
     * Check if a strategy exists for the specified interface type
     * 
     * @param interfaceType The interface type
     * @return true if a strategy exists, false otherwise
     */
    public boolean hasStrategy(String interfaceType) {
        return strategyMap.containsKey(interfaceType.toUpperCase());
    }
}
