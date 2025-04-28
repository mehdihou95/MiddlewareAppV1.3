package com.middleware.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration class for caching in the application.
 * Defines cache names and cache manager configuration.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Creates a cache manager with predefined cache names.
     *
     * @return The configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "clients",
            "interfaces",
            "mappingRules",
            "processedFiles",
            "users",
            "auditLogs",
            "asnHeaders",
            "asnLines",
            "xsdSchemas"
        ));
        return cacheManager;
    }
} 
