package com.middleware.shared.security.health;

import com.middleware.shared.security.service.JwtService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SecurityHealthIndicator implements HealthIndicator {
    
    private final JwtService jwtService;
    
    public SecurityHealthIndicator(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    public Health health() {
        try {
            // Verify JWT service is working
            String testToken = jwtService.generateTestToken();
            boolean isValid = jwtService.validateTestToken(testToken);
            
            if (isValid) {
                return Health.up()
                    .withDetail("jwtService", "operational")
                    .build();
            } else {
                return Health.down()
                    .withDetail("jwtService", "validation failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("jwtService", "failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
} 
