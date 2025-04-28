package com.middleware.listener.connectors.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    @NotNull
    private String contextPath = "/api/v1";

    @Positive
    private int port = 8081;

    private String[] allowedOrigins = {"*"};

    @NotNull
    private String processedDirectory = "processed";

    @NotNull
    private String errorDirectory = "error";

    @Positive
    private int maxRetries = 3;

    @Positive
    private long retryDelay = 5000;

    // Security settings
    private boolean enableSecurity = true;
    private String jwtSecret;
    private long jwtExpirationMs = 86400000; // 24 hours

    // Rate limiting
    private boolean enableRateLimit = true;
    private int rateLimit = 100;
    private int rateLimitPeriodSeconds = 60;

    // Getters and Setters
    public String getContextPath() { return contextPath; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String[] getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(String[] allowedOrigins) { this.allowedOrigins = allowedOrigins; }

    public String getProcessedDirectory() { return processedDirectory; }
    public void setProcessedDirectory(String processedDirectory) { this.processedDirectory = processedDirectory; }

    public String getErrorDirectory() { return errorDirectory; }
    public void setErrorDirectory(String errorDirectory) { this.errorDirectory = errorDirectory; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getRetryDelay() { return retryDelay; }
    public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }

    public boolean isEnableSecurity() { return enableSecurity; }
    public void setEnableSecurity(boolean enableSecurity) { this.enableSecurity = enableSecurity; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getJwtExpirationMs() { return jwtExpirationMs; }
    public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }

    public boolean isEnableRateLimit() { return enableRateLimit; }
    public void setEnableRateLimit(boolean enableRateLimit) { this.enableRateLimit = enableRateLimit; }

    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }

    public int getRateLimitPeriodSeconds() { return rateLimitPeriodSeconds; }
    public void setRateLimitPeriodSeconds(int rateLimitPeriodSeconds) { this.rateLimitPeriodSeconds = rateLimitPeriodSeconds; }
} 