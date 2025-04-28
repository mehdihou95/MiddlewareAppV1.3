package com.middleware.shared.security.service.impl;

import com.middleware.shared.security.service.JwtBlacklistService;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Primary
public class InMemoryJwtBlacklistService implements JwtBlacklistService {
    
    private final ConcurrentMap<String, Long> blacklist = new ConcurrentHashMap<>();
    
    @Override
    public void blacklistToken(String token) {
        // Store token with current time + 1 hour (default expiration)
        blacklist.put(token, System.currentTimeMillis() + 3600000);
    }
    
    @Override
    public boolean isBlacklisted(String token) {
        Long expirationTime = blacklist.get(token);
        if (expirationTime == null) {
            return false;
        }
        
        // Remove expired tokens
        if (System.currentTimeMillis() > expirationTime) {
            blacklist.remove(token);
            return false;
        }
        
        return true;
    }
} 
