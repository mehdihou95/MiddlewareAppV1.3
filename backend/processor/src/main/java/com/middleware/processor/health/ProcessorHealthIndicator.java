package com.middleware.processor.health;

import com.middleware.shared.health.ExternalServiceHealthIndicator;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.processor.metrics.ProcessingMetrics;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Health indicator for the processor module.
 * Extends ExternalServiceHealthIndicator to provide additional health checks specific to the processor.
 */
@Component
public class ProcessorHealthIndicator extends ExternalServiceHealthIndicator {

    private final ConnectionFactory rabbitConnectionFactory;
    private final CircuitBreakerService circuitBreakerService;
    private final ProcessingMetrics processingMetrics;
    private final Executor batchTaskExecutor;

    @Autowired
    public ProcessorHealthIndicator(
            ConnectionFactory rabbitConnectionFactory,
            CircuitBreakerService circuitBreakerService,
            ProcessingMetrics processingMetrics,
            Executor batchTaskExecutor) {
        super(processingMetrics.getRegistry());
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.circuitBreakerService = circuitBreakerService;
        this.processingMetrics = processingMetrics;
        this.batchTaskExecutor = batchTaskExecutor;

        // Register health checks
        registerHealthCheck("rabbitmq", this::checkRabbitMQ);
        registerHealthCheck("circuit-breakers", this::checkCircuitBreakers);
        registerHealthCheck("thread-pool", this::checkThreadPool);
        registerHealthCheck("processing-metrics", this::checkProcessingMetrics);
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        super.doHealthCheck(builder);
    }

    private HealthCheck.Result checkRabbitMQ() {
        try {
            rabbitConnectionFactory.createConnection().close();
            return new HealthCheck.Result(true, "UP", "RabbitMQ connection is healthy");
        } catch (Exception e) {
            return new HealthCheck.Result(false, "DOWN", "RabbitMQ connection failed: " + e.getMessage());
        }
    }

    private HealthCheck.Result checkCircuitBreakers() {
        try {
            String state = circuitBreakerService.getCircuitBreakerState();
            String metrics = circuitBreakerService.getCircuitBreakerMetrics();
            
            if ("CLOSED".equals(state)) {
                return new HealthCheck.Result(true, "UP", "Circuit breakers are healthy: " + metrics);
            } else {
                return new HealthCheck.Result(false, state, "Circuit breakers are not healthy: " + metrics);
            }
        } catch (Exception e) {
            return new HealthCheck.Result(false, "ERROR", "Failed to check circuit breakers: " + e.getMessage());
        }
    }

    private HealthCheck.Result checkThreadPool() {
        if (batchTaskExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) batchTaskExecutor;
            int activeThreads = executor.getActiveCount();
            int poolSize = executor.getPoolSize();
            int maxPoolSize = executor.getMaximumPoolSize();
            int queueSize = executor.getQueue().size();
            
            String status = activeThreads < maxPoolSize ? "UP" : "WARNING";
            String message = String.format(
                "Thread pool status: %d/%d active threads, queue size: %d",
                activeThreads, maxPoolSize, queueSize
            );
            
            return new HealthCheck.Result(
                activeThreads < maxPoolSize,
                status,
                message
            );
        }
        return new HealthCheck.Result(true, "UP", "Thread pool is healthy");
    }

    private HealthCheck.Result checkProcessingMetrics() {
        try {
            // Check if we're processing messages
            double processedCount = processingMetrics.getCounters().processingSuccess("xmlProcessing").count();
            double errorCount = processingMetrics.getCounters().processingErrors("xmlProcessing").count();
            
            String status = errorCount == 0 ? "UP" : "WARNING";
            String message = String.format(
                "Processing metrics: %.0f processed, %.0f errors",
                processedCount, errorCount
            );
            
            return new HealthCheck.Result(
                errorCount == 0,
                status,
                message
            );
        } catch (Exception e) {
            return new HealthCheck.Result(false, "ERROR", "Failed to check processing metrics: " + e.getMessage());
        }
    }
} 
