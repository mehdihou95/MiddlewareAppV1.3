package com.middleware.processor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProcessingMetrics {
    private final MeterRegistry registry;
    private final ProcessingTimers timers;
    private final ProcessingCounters counters;
    
    public ProcessingMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.timers = new ProcessingTimers(registry);
        this.counters = new ProcessingCounters(registry);
    }
    
    public ProcessingTimers getTimers() {
        return timers;
    }
    
    public ProcessingCounters getCounters() {
        return counters;
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
    }
} 