package com.middleware.processor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessingMetrics {
    private final MeterRegistry registry;
    private final ProcessingTimers timers;
    private final ProcessingCounters counters;
    private final BatchMetrics batchMetrics;
    private final QueueMetrics queueMetrics;
    
    public ProcessingMetrics(MeterRegistry registry, RabbitAdmin rabbitAdmin, 
            @Qualifier("asnHighPriorityQueue") Queue inboundQueue) {
        this.registry = registry;
        this.timers = new ProcessingTimers(registry);
        this.counters = new ProcessingCounters(registry);
        this.batchMetrics = new BatchMetrics(registry);
        this.queueMetrics = new QueueMetrics(registry, rabbitAdmin, inboundQueue);
    }
    
    public MeterRegistry getRegistry() {
        return registry;
    }
    
    public ProcessingTimers getTimers() {
        return timers;
    }
    
    public ProcessingCounters getCounters() {
        return counters;
    }
    
    public BatchMetrics getBatchMetrics() {
        return batchMetrics;
    }
    
    public QueueMetrics getQueueMetrics() {
        return queueMetrics;
    }
    
    @Component
    public static class ProcessingTimers {
        private final MeterRegistry registry;
        private final ConcurrentMap<String, Timer> documentProcessingTimers = new ConcurrentHashMap<>();
        
        private final Timer xmlParsingTimer;
        private final Timer validationTimer;
        private final Timer databaseOperationTimer;
        private final Timer transformationTimer;
        
        public ProcessingTimers(MeterRegistry registry) {
            this.registry = registry;
            
            this.xmlParsingTimer = Timer.builder("document.processing.xml.parse")
                .description("Time taken to parse XML documents")
                .register(registry);
                
            this.validationTimer = Timer.builder("document.processing.validation")
                .description("Time taken for document validation")
                .register(registry);
                
            this.databaseOperationTimer = Timer.builder("document.processing.database")
                .description("Time taken for database operations")
                .register(registry);
                
            this.transformationTimer = Timer.builder("document.processing.transformation")
                .description("Time taken for data transformations")
                .register(registry);
        }
        
        public Timer xmlParsingTimer() {
            return xmlParsingTimer;
        }
        
        public Timer validationTimer() {
            return validationTimer;
        }
        
        public Timer databaseOperationTimer() {
            return databaseOperationTimer;
        }
        
        public Timer transformationTimer() {
            return transformationTimer;
        }
        
        public Timer documentProcessingTimer(String strategyName) {
            return documentProcessingTimers.computeIfAbsent(strategyName,
                name -> Timer.builder("document.processing.strategy")
                    .tag("strategy", name)
                    .description("Time taken to process documents by strategy")
                    .register(registry)
            );
        }
    }
    
    @Component
    public static class ProcessingCounters {
        private final MeterRegistry registry;
        private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Counter> successCounters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Counter> priorityCounters = new ConcurrentHashMap<>();
        
        public ProcessingCounters(MeterRegistry registry) {
            this.registry = registry;
        }
        
        public Counter processingErrors(String strategyName) {
            return errorCounters.computeIfAbsent(strategyName,
                name -> Counter.builder("document.processing.errors")
                    .tag("strategy", name)
                    .description("Number of processing errors by strategy")
                    .register(registry)
            );
        }
        
        public Counter processingSuccess(String strategyName) {
            return successCounters.computeIfAbsent(strategyName,
                name -> Counter.builder("document.processing.success")
                    .tag("strategy", name)
                    .description("Number of successfully processed documents by strategy")
                    .register(registry)
            );
        }
        
        public Counter processedMessages(String priority) {
            return priorityCounters.computeIfAbsent(priority,
                p -> Counter.builder("document.processing.priority")
                    .tag("priority", p)
                    .description("Number of processed messages by priority")
                    .register(registry)
            );
        }
    }
    
    @Component
    public static class BatchMetrics {
        private final Counter batchProcessingCounter;
        private final Counter batchFailureCounter;
        private final Counter documentProcessingCounter;
        private final Counter documentFailureCounter;
        private final Counter partialBatchFailureCounter;
        private final Timer batchProcessingTimer;
        
        public BatchMetrics(MeterRegistry registry) {
            this.batchProcessingCounter = registry.counter("processor.batch.processing");
            this.batchFailureCounter = registry.counter("processor.batch.failure");
            this.documentProcessingCounter = registry.counter("processor.document.processing");
            this.documentFailureCounter = registry.counter("processor.document.failure");
            this.partialBatchFailureCounter = registry.counter("processor.batch.partial.failure");
            this.batchProcessingTimer = registry.timer("processor.batch.processing.time");
        }
        
        public void recordBatchProcessing(long startTime) {
            batchProcessingCounter.increment();
            batchProcessingTimer.record(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS);
        }
        
        public void recordBatchFailure() {
            batchFailureCounter.increment();
        }
        
        public void recordDocumentProcessing() {
            documentProcessingCounter.increment();
        }
        
        public void recordDocumentFailure() {
            documentFailureCounter.increment();
        }
        
        public void recordPartialBatchFailure(int successCount, int failureCount) {
            partialBatchFailureCounter.increment();
            documentProcessingCounter.increment(successCount);
            documentFailureCounter.increment(failureCount);
        }
    }
    
    @Component
    public static class QueueMetrics {
        private final RabbitAdmin rabbitAdmin;
        private final MeterRegistry meterRegistry;
        private final Queue inboundQueue;
        
        public QueueMetrics(MeterRegistry meterRegistry, RabbitAdmin rabbitAdmin, @Qualifier("asnHighPriorityQueue") Queue inboundQueue) {
            this.rabbitAdmin = rabbitAdmin;
            this.meterRegistry = meterRegistry;
            this.inboundQueue = inboundQueue;
        }
        
        @Scheduled(fixedRate = 5000)
        public void monitorQueueDepth() {
            int messageCount = rabbitAdmin.getQueueInfo(inboundQueue.getName()).getMessageCount();
            meterRegistry.gauge("rabbitmq.queue.depth", 
                Tags.of("queue", inboundQueue.getName()),
                messageCount);
        }
    }
} 
