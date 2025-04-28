package com.middleware.processor.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityLoggerServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggerServiceImpl.class);

    public void logSuccessfulLogin(String username, String ipAddress) {
        logger.info("Successful login - User: {}, IP: {}", username, ipAddress);
    }

    public void logFailedLogin(String username, String ipAddress) {
        logger.warn("Failed login attempt - User: {}, IP: {}", username, ipAddress);
    }

    public void logTokenValidation(String username, boolean isValid, String ipAddress) {
        if (isValid) {
            logger.info("Token validated successfully - User: {}, IP: {}", username, ipAddress);
        } else {
            logger.warn("Invalid token detected - User: {}, IP: {}", username, ipAddress);
        }
    }

    public void logTokenRefresh(String username, boolean success, String ipAddress) {
        if (success) {
            logger.info("Token refreshed successfully - User: {}, IP: {}", username, ipAddress);
        } else {
            logger.warn("Token refresh failed - User: {}, IP: {}", username, ipAddress);
        }
    }

    public void logLogout(String username, String ipAddress) {
        logger.info("User logged out - User: {}, IP: {}", username, ipAddress);
    }

    public void logSuspiciousActivity(String username, String activity, String ipAddress) {
        logger.warn("Suspicious activity detected - User: {}, Activity: {}, IP: {}", username, activity, ipAddress);
    }
} 
