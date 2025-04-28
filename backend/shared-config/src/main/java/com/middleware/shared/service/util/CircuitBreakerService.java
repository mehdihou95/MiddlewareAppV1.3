package com.middleware.shared.service.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Service for handling circuit breaker operations.
 * Provides methods to execute repository operations with circuit breaker protection.
 */
@Service
public class CircuitBreakerService {
    
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    @Autowired
    private CircuitBreakerConfig repositoryCircuitBreakerConfig;
    
    private final CircuitBreaker circuitBreaker;
    
    public CircuitBreakerService(CircuitBreakerConfig repositoryCircuitBreakerConfig) {
        this.repositoryCircuitBreakerConfig = repositoryCircuitBreakerConfig;
        this.circuitBreaker = CircuitBreaker.of("repositoryCircuitBreaker", repositoryCircuitBreakerConfig);
    }
    
    /**
     * Execute a repository operation with circuit breaker protection.
     *
     * @param operation The operation to execute
     * @param fallback The fallback operation if the circuit breaker is open
     * @return The result of the operation or fallback
     * @param <T> The type of the result
     */
    @SuppressWarnings("unchecked")
    public <T> T executeRepositoryOperation(Supplier<T> operation, Supplier<T> fallback) {
        try {
            return (T) CircuitBreaker.decorateSupplier(circuitBreaker, operation).get();
        } catch (Exception e) {
            log.warn("Circuit breaker fallback triggered: {}", e.getMessage());
            return (T) fallback.get();
        }
    }
    
    /**
     * Execute a void repository operation with circuit breaker protection.
     *
     * @param operation The operation to execute
     * @param fallback The fallback operation if the circuit breaker is open
     */
    public void executeVoidRepositoryOperation(VoidOperation operation, VoidOperation fallback) {
        try {
            CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
                try {
                    operation.execute();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).run();
        } catch (Exception e) {
            log.warn("Circuit breaker fallback triggered: {}", e.getMessage());
            try {
                fallback.execute();
            } catch (Exception fallbackError) {
                log.error("Fallback operation failed: {}", fallbackError.getMessage());
                throw new RuntimeException(fallbackError);
            }
        }
    }
    
    /**
     * Functional interface for void operations that may throw exceptions.
     */
    @FunctionalInterface
    public interface VoidOperation {
        void execute() throws Exception;
    }
} 