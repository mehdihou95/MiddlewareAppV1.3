package com.example.processor.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BatchProcessingMetrics {

    private final Counter processedBatches;
    private final Counter failedBatches;
    private final Timer batchProcessingTime;
    private final Counter processedDocuments;
    private final Counter failedDocuments;

    public BatchProcessingMetrics(MeterRegistry registry) {
        processedBatches = registry.counter("processor.batches.processed");
        failedBatches = registry.counter("processor.batches.failed");
        batchProcessingTime = registry.timer("processor.batch.processing.time");
        processedDocuments = registry.counter("processor.documents.processed");
        failedDocuments = registry.counter("processor.documents.failed");
    }

    public void recordBatchProcessing(long startTime) {
        processedBatches.increment();
        batchProcessingTime.record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
    }

    public void recordBatchFailure() {
        failedBatches.increment();
    }

    public void recordDocumentProcessing() {
        processedDocuments.increment();
    }

    public void recordDocumentFailure() {
        failedDocuments.increment();
    }
} 