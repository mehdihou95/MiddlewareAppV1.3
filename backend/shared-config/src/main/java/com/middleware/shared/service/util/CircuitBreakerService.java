package com.middleware.shared.service.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.function.Supplier;

/**
 * Enhanced service for handling circuit breaker operations.
 * Provides methods to execute repository operations with circuit breaker protection.
 * Includes monitoring and alerting for circuit breaker state changes.
 */
@Service
public class CircuitBreakerService {
    
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    @Autowired
    private CircuitBreakerConfig repositoryCircuitBreakerConfig;
    
    private final CircuitBreaker circuitBreaker;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public CircuitBreakerService(CircuitBreakerConfig repositoryCircuitBreakerConfig) {
        this.repositoryCircuitBreakerConfig = repositoryCircuitBreakerConfig;
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(repositoryCircuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("repositoryCircuitBreaker");
        
        // Register event listeners for monitoring and alerting
        setupCircuitBreakerEventListeners();
    }
    
    @PostConstruct
    public void init() {
        log.info("CircuitBreakerService initialized with config: {}", repositoryCircuitBreakerConfig);
    }
    
    /**
     * Setup event listeners for circuit breaker state transitions.
     * This enables monitoring and alerting when the circuit breaker changes state.
     */
    private void setupCircuitBreakerEventListeners() {
        circuitBreaker.getEventPublisher()
            .onStateTransition(this::handleStateTransition);
    }
    
    /**
     * Handle circuit breaker state transition events.
     * Logs state changes and could trigger alerts in a production environment.
     *
     * @param event The state transition event
     */
    private void handleStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("Circuit breaker '{}' state changed from {} to {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());
        
        // In a production environment, you might want to:
        // 1. Send alerts to monitoring systems
        // 2. Notify operations team
        // 3. Update health indicators
        // 4. Log to a specialized monitoring service
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
            return CircuitBreaker.decorateSupplier(circuitBreaker, operation).get();
        } catch (Exception e) {
            log.warn("Circuit breaker fallback triggered: {}", e.getMessage());
            return fallback.get();
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
     * Get the current state of the circuit breaker.
     *
     * @return The current state as a string
     */
    public String getCircuitBreakerState() {
        return circuitBreaker.getState().name();
    }
    
    /**
     * Get metrics for the circuit breaker.
     *
     * @return A string representation of the circuit breaker metrics
     */
    public String getCircuitBreakerMetrics() {
        return String.format(
            "Failure Rate: %.2f%%, Slow Call Rate: %.2f%%, Number of failed calls: %d, Number of slow calls: %d",
            circuitBreaker.getMetrics().getFailureRate(),
            circuitBreaker.getMetrics().getSlowCallRate(),
            circuitBreaker.getMetrics().getNumberOfFailedCalls(),
            circuitBreaker.getMetrics().getNumberOfSlowCalls()
        );
    }
    
    /**
     * Reset the circuit breaker to its closed state.
     * This should be used with caution, typically only in testing or when you're certain
     * the underlying issues have been resolved.
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
        log.info("Circuit breaker has been manually reset to CLOSED state");
    }
    
    /**
     * Functional interface for void operations that may throw exceptions.
     */
    @FunctionalInterface
    public interface VoidOperation {
        void execute() throws Exception;
    }
}
