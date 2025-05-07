package com.middleware.shared.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseHealthConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public HealthIndicator databaseHealthIndicator() {
        return () -> {
            try {
                // Check database connection
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                
                // Check if we can access the schema_version table (Flyway)
                try {
                    jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM schema_version", 
                        Integer.class
                    );
                    return Health.up()
                        .withDetail("message", "Database connection and Flyway schema are healthy")
                        .build();
                } catch (Exception e) {
                    log.warn("Flyway schema_version table not found: {}", e.getMessage());
                    return Health.up()
                        .withDetail("message", "Database connection is healthy but Flyway schema is not initialized")
                        .build();
                }
            } catch (Exception e) {
                log.error("Database health check failed: {}", e.getMessage());
                return Health.down()
                    .withException(e)
                    .build();
            }
        };
    }
} 