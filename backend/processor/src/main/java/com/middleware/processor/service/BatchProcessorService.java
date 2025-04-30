package com.middleware.processor.service;

import com.middleware.processor.model.MessageContent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BatchProcessorService {
    /**
     * Process a batch of messages asynchronously
     * @param batch List of messages to process
     * @return List of CompletableFuture for tracking processing status
     */
    List<CompletableFuture<Void>> processBatch(List<MessageContent> batch);
} 
