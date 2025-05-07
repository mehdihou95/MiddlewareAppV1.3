package com.middleware.shared.health;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base health indicator for external services.
 * Provides common health checks and metrics integration.
 */
@Component
public class ExternalServiceHealthIndicator extends AbstractHealthIndicator {

    private final MeterRegistry meterRegistry;
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();

    public ExternalServiceHealthIndicator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            boolean allHealthy = true;
            StringBuilder statusMessage = new StringBuilder();

            // Execute all registered health checks
            for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
                String serviceName = entry.getKey();
                HealthCheck healthCheck = entry.getValue();
                
                try {
                    HealthCheck.Result result = healthCheck.check();
                    builder.withDetail(serviceName, result.getStatus());
                    statusMessage.append(serviceName).append(": ").append(result.getStatus()).append(", ");
                    
                    if (!result.isHealthy()) {
                        allHealthy = false;
                    }
                    
                    // Record health check metrics
                    meterRegistry.gauge("health.check." + serviceName, 
                        result.isHealthy() ? 1 : 0);
                    
                } catch (Exception e) {
                    builder.withDetail(serviceName, "ERROR")
                          .withException(e);
                    statusMessage.append(serviceName).append(": ERROR, ");
                    allHealthy = false;
                }
            }

            if (allHealthy) {
                builder.up()
                    .withDetail("status", "UP")
                    .withDetail("message", statusMessage.toString());
            } else {
                builder.down()
                    .withDetail("status", "DOWN")
                    .withDetail("message", statusMessage.toString());
            }
        } catch (Exception e) {
            builder.down()
                .withException(e)
                .withDetail("status", "DOWN")
                .withDetail("message", "Health check failed: " + e.getMessage());
        }
    }

    /**
     * Register a new health check.
     * @param name The name of the health check
     * @param healthCheck The health check implementation
     */
    public void registerHealthCheck(String name, HealthCheck healthCheck) {
        healthChecks.put(name, healthCheck);
    }

    /**
     * Remove a health check.
     * @param name The name of the health check to remove
     */
    public void removeHealthCheck(String name) {
        healthChecks.remove(name);
    }

    /**
     * Functional interface for health checks.
     */
    @FunctionalInterface
    public interface HealthCheck {
        Result check();

        class Result {
            private final boolean healthy;
            private final String status;
            private final String message;

            public Result(boolean healthy, String status, String message) {
                this.healthy = healthy;
                this.status = status;
                this.message = message;
            }

            public boolean isHealthy() {
                return healthy;
            }

            public String getStatus() {
                return status;
            }

            public String getMessage() {
                return message;
            }
        }
    }
} 
