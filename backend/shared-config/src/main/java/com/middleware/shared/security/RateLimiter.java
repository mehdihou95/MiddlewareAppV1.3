package com.middleware.shared.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_SIZE_MS = 300000; // 5 minutes
    
    private final ConcurrentMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class AttemptInfo {
        final AtomicInteger count;
        final Instant windowStart;

        AttemptInfo() {
            this.count = new AtomicInteger(1);
            this.windowStart = Instant.now();
        }
    }

    public boolean checkRateLimit(String key) {
        cleanup();
        
        AttemptInfo info = attempts.compute(key, (k, v) -> {
            if (v == null || isWindowExpired(v.windowStart)) {
                return new AttemptInfo();
            }
            v.count.incrementAndGet();
            return v;
        });

        int attemptCount = info.count.get();
        if (attemptCount > MAX_ATTEMPTS) {
            logger.warn("Rate limit exceeded for key: {}. Attempts: {}", key, attemptCount);
            return false;
        }

        logger.debug("Rate limit check for key: {}. Attempts: {}/{}", key, attemptCount, MAX_ATTEMPTS);
        return true;
    }

    public void resetLimit(String key) {
        attempts.remove(key);
        logger.debug("Rate limit reset for key: {}", key);
    }

    private boolean isWindowExpired(Instant windowStart) {
        return Instant.now().isAfter(windowStart.plusMillis(WINDOW_SIZE_MS));
    }

    private void cleanup() {
        attempts.entrySet().removeIf(entry -> isWindowExpired(entry.getValue().windowStart));
    }
} 
