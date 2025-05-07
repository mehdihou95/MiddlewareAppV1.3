package com.middleware.shared.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EnhancedRateLimiter {
    private static final int PRIMARY_MAX_ATTEMPTS = 5;
    private static final long PRIMARY_WINDOW_MS = 300000; // 5 minutes
    private static final long SECONDARY_WINDOW_MS = 3600000; // 1 hour
    
    private static class AttemptInfo {
        int count;
        long windowStart;
    }

    private final ConcurrentMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EnhancedRateLimiter() {
        // Schedule cleanup every hour
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }

    public boolean checkRateLimit(String key) {
        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());
        
        if (isWindowExpired(info)) {
            info.count = 0;
            info.windowStart = System.currentTimeMillis();
        }
        
        return info.count < PRIMARY_MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String key) {
        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());
        
        if (isWindowExpired(info)) {
            info.count = 0;
            info.windowStart = System.currentTimeMillis();
        }
        
        info.count++;
        
        if (info.count >= PRIMARY_MAX_ATTEMPTS) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
    }

    public void resetLimit(String key) {
        attempts.remove(key);
    }

    private boolean isWindowExpired(AttemptInfo info) {
        return System.currentTimeMillis() - info.windowStart > PRIMARY_WINDOW_MS;
    }

    private void cleanup() {
        attempts.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue().windowStart > SECONDARY_WINDOW_MS);
    }
} 
