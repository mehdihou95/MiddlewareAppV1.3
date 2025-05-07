package com.middleware.processor.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ValidationResultCache {

    private static final String CACHE_PREFIX = "validation:";
    private static final long CACHE_TTL = 3600; // 1 hour

    private final RedisTemplate<String, Boolean> redisTemplate;

    public ValidationResultCache(RedisTemplate<String, Boolean> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean getValidationResult(String documentHash) {
        return redisTemplate.opsForValue().get(CACHE_PREFIX + documentHash);
    }

    public void cacheValidationResult(String documentHash, boolean isValid) {
        redisTemplate.opsForValue().set(
            CACHE_PREFIX + documentHash,
            isValid,
            CACHE_TTL,
            TimeUnit.SECONDS
        );
    }

    public void invalidateCache(String documentHash) {
        redisTemplate.delete(CACHE_PREFIX + documentHash);
    }

    public void clearCache() {
        var keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }
} 
