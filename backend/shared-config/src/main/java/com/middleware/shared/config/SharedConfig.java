package com.middleware.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Base configuration class containing common settings for all modules.
 * Provides shared configurations for database access, transaction management, and other common features.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.middleware.shared.repository")
@EnableTransactionManagement
public class SharedConfig {
    // Common configurations will be added here as needed
} 