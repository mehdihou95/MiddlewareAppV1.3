package com.middleware.processor.service.impl;

import com.middleware.processor.service.BatchProcessorService;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.processor.model.MessageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${batch.size:100}")
    private int batchSize;

    @Value("${batch.timeout-seconds:300}")
    private int batchTimeoutSeconds;

    @Override
    public List<CompletableFuture<Void>> processBatch(List<MessageContent> batch) {
        log.info("Starting batch processing for {} messages", batch.size());

        List<List<MessageContent>> batches = partition(batch, batchSize);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (List<MessageContent> batchSegment : batches) {
            CompletableFuture<Void> batchFuture = CompletableFuture
                .runAsync(() -> processBatchSegment(batchSegment), batchTaskExecutor);
            futures.add(batchFuture);
        }

        return futures;
    }

    @Transactional
    protected void processBatchSegment(List<MessageContent> batchSegment) {
        for (MessageContent message : batchSegment) {
            try {
                xmlProcessorService.processXmlFile(message.getMultipartFile(), message.getInterfaceEntity());
                log.debug("Processed message: {}", message.getMultipartFile().getOriginalFilename());
            } catch (Exception e) {
                log.error("Error processing message: {}", message.getMultipartFile().getOriginalFilename(), e);
                // Continue processing other messages in batch
            }
        }

        log.info("Completed batch segment processing. Success: {}, Failed: {}", 
            batchSegment.size(), batchSegment.size());
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