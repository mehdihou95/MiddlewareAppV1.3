package com.middleware.shared.repository.config;

import com.middleware.shared.exception.BaseValidationException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import com.middleware.shared.exception.ValidationException;

/**
 * Configuration class for repository resilience patterns.
 * Provides circuit breaker, retry, and time limiter configurations for repository operations.
 */
@Configuration
@EnableRetry
public class RepositoryResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(RepositoryResilienceConfig.class);

    /**
     * Circuit breaker configuration for repository operations.
     * Configures failure thresholds, wait durations, and exception handling.
     */
    @Bean
    public CircuitBreakerConfig repositoryCircuitBreakerConfig() {
        log.info("Configuring repository circuit breaker with custom settings");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(30)
                .slowCallRateThreshold(40)
                .slowCallDurationThreshold(Duration.ofMillis(500))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .recordExceptions(
                    DataAccessException.class,
                    TimeoutException.class,
                    QueryTimeoutException.class,
                    TransientDataAccessException.class,
                    SQLException.class,
                    SQLTimeoutException.class
                )
                .ignoreExceptions(
                    BaseValidationException.class,
                    IllegalArgumentException.class
                )
                .build();
    }

    @Bean
    public CircuitBreakerConfig fileProcessingCircuitBreakerConfig() {
        log.info("Configuring file processing circuit breaker with custom settings");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(20)  // More conservative for file processing
                .slowCallRateThreshold(30)
                .slowCallDurationThreshold(Duration.ofSeconds(2))  // Longer duration for file processing
                .waitDurationInOpenState(Duration.ofSeconds(60))  // Longer wait time
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                    DataAccessException.class,
                    TimeoutException.class,
                    IOException.class,
                    XMLStreamException.class
                )
                .ignoreExceptions(
                    BaseValidationException.class,
                    IllegalArgumentException.class
                )
                .build();
    }

    @Bean
    public CircuitBreakerConfig validationCircuitBreakerConfig() {
        log.info("Configuring validation circuit breaker with custom settings");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(40)  // Less conservative for validation
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofMillis(200))  // Shorter duration for validation
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .slidingWindowSize(15)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                    ValidationException.class,
                    DataAccessException.class
                )
                .ignoreExceptions(
                    BaseValidationException.class,
                    IllegalArgumentException.class
                )
                .build();
    }

    /**
     * Time limiter configuration for repository operations.
     * Prevents long-running database operations.
     */
    @Bean
    public TimeLimiterConfig repositoryTimeLimiterConfig() {
        log.info("Configuring repository time limiter with 1 second timeout");
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build();
    }

    /**
     * Retry template for repository operations.
     * Implements exponential backoff and specific exception handling.
     */
    @Bean
    public RetryTemplate repositoryRetryTemplate() {
        log.info("Configuring repository retry template with exponential backoff");
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMaxInterval(1500);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure retry policy with specific exceptions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(DataAccessException.class, true);
        retryableExceptions.put(TransientDataAccessException.class, true);
        retryableExceptions.put(QueryTimeoutException.class, true);
        retryableExceptions.put(TimeoutException.class, true);
        retryableExceptions.put(Exception.class, false);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        log.debug("Retry template configured with {} retryable exceptions", retryableExceptions.size());
        return retryTemplate;
    }
}
