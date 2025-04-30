package com.middleware.processor.config;

import com.middleware.shared.config.ResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration class for Circuit Breaker pattern implementation.
 * Extends shared ResilienceConfig and adds processor-specific settings.
 */
@Configuration
public class CircuitBreakerConfiguration extends ResilienceConfig {

    /**
     * Circuit breaker configuration for repository operations.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> repositoryCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(40)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(3)
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build()), "repository");
    }

    /**
     * Circuit breaker configuration for XML processing operations.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> xmlProcessingCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(30)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build()), "xmlProcessing");
    }

    /**
     * Circuit breaker configuration for file processing operations.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> fileProcessingCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(30)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build()), "fileProcessing");
    }

    /**
     * Retry configuration for database operations.
     */
    @Bean
    public Retry databaseRetry(RetryRegistry registry) {
        return registry.retry("database", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(IOException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build());
    }

    /**
     * Retry configuration for file processing operations.
     */
    @Bean
    public Retry fileProcessingRetry(RetryRegistry registry) {
        return registry.retry("fileProcessing", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(IOException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build());
    }

    /**
     * Circuit breaker for repository operations.
     */
    @Bean
    public CircuitBreaker repositoryCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("repository");
    }

    /**
     * Circuit breaker for XML processing operations.
     */
    @Bean
    public CircuitBreaker xmlProcessingCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("xmlProcessing");
    }

    /**
     * Circuit breaker for file processing operations.
     */
    @Bean
    public CircuitBreaker fileProcessingCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("fileProcessing");
    }
}
