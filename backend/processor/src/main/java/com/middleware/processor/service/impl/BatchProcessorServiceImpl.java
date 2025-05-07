package com.middleware.processor.service.impl;

import com.middleware.processor.service.BatchProcessorService;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.processor.model.MessageContent;
import com.middleware.processor.metrics.ProcessingMetrics;
import com.middleware.processor.exception.ProcessingException;
import com.middleware.shared.service.util.CircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessorServiceImpl implements BatchProcessorService {

    private final Executor batchTaskExecutor;
    private final XmlProcessorService xmlProcessorService;
    private final ProcessingMetrics processingMetrics;
    private final CircuitBreakerService circuitBreakerService;

    @Value("${batch.size:100}")
    private int batchSize;

    @Value("${batch.timeout-seconds:300}")
    private int batchTimeoutSeconds;

    @Value("${batch.max-retries:3}")
    private int maxRetries;

    @Override
    public List<CompletableFuture<Void>> processBatch(List<MessageContent> batch) {
        log.info("Starting batch processing for {} messages", batch.size());
        long startTime = System.currentTimeMillis();

        List<List<MessageContent>> batches = partition(batch, batchSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (List<MessageContent> batchSegment : batches) {
            CompletableFuture<Void> batchFuture = CompletableFuture
                .runAsync(() -> {
                    try {
                        processBatchSegment(batchSegment);
                        processingMetrics.getBatchMetrics().recordBatchProcessing(startTime);
                        processingMetrics.getBatchMetrics().recordDocumentProcessing();
                    } catch (Exception e) {
                        processingMetrics.getBatchMetrics().recordBatchFailure();
                        processingMetrics.getBatchMetrics().recordDocumentFailure();
                        log.error("Batch processing failed: {}", e.getMessage(), e);
                        throw new ProcessingException("Batch processing failed", e);
                    }
                }, batchTaskExecutor);
            futures.add(batchFuture);
        }

        return futures;
    }

    protected void processBatchSegment(List<MessageContent> batchSegment) {
        int successCount = 0;
        int failureCount = 0;

        for (MessageContent message : batchSegment) {
            try {
                processSingleMessage(message);
                successCount++;
                log.debug("Processed message: {}", message.getMultipartFile().getOriginalFilename());
            } catch (Exception e) {
                failureCount++;
                log.error("Error processing message: {}", message.getMultipartFile().getOriginalFilename(), e);
                // Continue processing other messages in batch
            }
        }

        log.info("Completed batch segment processing. Success: {}, Failed: {}", 
            successCount, failureCount);
        
        if (failureCount > 0) {
            processingMetrics.getBatchMetrics().recordPartialBatchFailure(successCount, failureCount);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        value = {ProcessingException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    protected void processSingleMessage(MessageContent message) {
        try {
            circuitBreakerService.executeRepositoryOperation(
                () -> {
                    xmlProcessorService.processXmlFile(message.getMultipartFile(), message.getInterfaceEntity());
                    return null;
                },
                () -> {
                    log.warn("Circuit breaker fallback triggered for message: {}", message.getMultipartFile().getOriginalFilename());
                    return null;
                }
            );
            processingMetrics.getCounters().processingSuccess("xmlProcessing").increment();
        } catch (Exception e) {
            processingMetrics.getCounters().processingErrors("xmlProcessing").increment();
            throw new ProcessingException("Failed to process message: " + e.getMessage(), e);
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(list.stream()
            .collect(Collectors.groupingBy(item -> list.indexOf(item) / size))
            .values());
    }
} 
