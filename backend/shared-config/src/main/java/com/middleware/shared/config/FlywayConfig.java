package com.middleware.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlywayConfig {

    private final Environment environment;

    @Value("${spring.application.name}")
    private String moduleName;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                // Set module-specific migration location
                String migrationPath = String.format("classpath:db/migration/%s", moduleName);
                Flyway.configure()
                    .locations(migrationPath)
                    .load()
                    .migrate();
                
                // Log migration info
                log.info("Running Flyway migrations for module: {}", moduleName);
                log.info("Migration locations: {}", migrationPath);
                
                log.info("Flyway migrations completed successfully for module: {}", moduleName);
            } catch (Exception e) {
                log.error("Error during Flyway migration for module {}: {}", moduleName, e.getMessage(), e);
                throw e;
            }
        };
    }
} 
