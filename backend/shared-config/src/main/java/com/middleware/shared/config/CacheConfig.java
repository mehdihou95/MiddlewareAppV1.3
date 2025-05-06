package com.middleware.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;

/**
 * Configuration class for caching in the application.
 * Supports both in-memory and Redis caching with proper configuration.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${spring.redis.enabled:false}")
    private boolean redisEnabled;
    
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    // Cache names used across the application
    public static final String[] CACHE_NAMES = {
        "clients",
        "interfaces",
        "mappingRules",
        "processedFiles",
        "users",
        "auditLogs",
        "asnHeaders",
        "asnLines",
        "xsdSchemas"
    };
    
    /**
     * Primary cache manager using ConcurrentMap for in-memory caching.
     * Used when Redis is not enabled or for non-distributed caching needs.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(CACHE_NAMES));
        return cacheManager;
    }
    
    /**
     * Redis connection factory for distributed caching.
     * Only created when Redis is enabled.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (!redisEnabled) {
            throw new IllegalStateException("Redis is not enabled. Please set redis.enabled=true to use Redis caching.");
        }
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }
    
    /**
     * Redis template for distributed caching operations.
     * Only created when Redis is enabled.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        if (!redisEnabled || connectionFactory == null) {
            return null;
        }
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis template specifically for validation results using Boolean values.
     * Only created when Redis is enabled.
     */
    @Bean
    public RedisTemplate<String, Boolean> validationResultRedisTemplate(RedisConnectionFactory connectionFactory) {
        if (!redisEnabled || connectionFactory == null) {
            return null;
        }
        
        RedisTemplate<String, Boolean> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
} 
