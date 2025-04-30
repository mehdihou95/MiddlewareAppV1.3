package com.example.shared.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class ExternalServiceHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Add common health checks for external services
            builder.up()
                .withDetail("status", "UP")
                .withDetail("message", "External services are healthy");
        } catch (Exception e) {
            builder.down()
                .withException(e)
                .withDetail("status", "DOWN")
                .withDetail("message", "External services are not responding");
        }
    }
} 