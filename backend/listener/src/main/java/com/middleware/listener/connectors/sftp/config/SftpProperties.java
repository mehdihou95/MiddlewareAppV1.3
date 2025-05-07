package com.middleware.listener.connectors.sftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Configuration
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {
    @NotNull
    private String host;
    
    @Positive
    private int port = 22;
    
    @NotNull
    private String username;
    
    private String password;
    private String privateKeyPath;
    private String privateKeyPassphrase;
    private boolean strictHostKeyChecking = false;
    
    @Positive
    private int connectionTimeout = 30000;
    
    @NotEmpty
    private String[] monitoredDirectories;
    
    @Positive
    private long pollingInterval = 60000;
    
    @Positive
    private int maxRetries = 3;
    
    @Positive
    private long retryDelay = 5000;

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    
    public String getPrivateKeyPassphrase() { return privateKeyPassphrase; }
    public void setPrivateKeyPassphrase(String privateKeyPassphrase) { this.privateKeyPassphrase = privateKeyPassphrase; }
    
    public boolean isStrictHostKeyChecking() { return strictHostKeyChecking; }
    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) { this.strictHostKeyChecking = strictHostKeyChecking; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public String[] getMonitoredDirectories() { return monitoredDirectories; }
    public void setMonitoredDirectories(String[] monitoredDirectories) { this.monitoredDirectories = monitoredDirectories; }
    
    public long getPollingInterval() { return pollingInterval; }
    public void setPollingInterval(long pollingInterval) { this.pollingInterval = pollingInterval; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getRetryDelay() { return retryDelay; }
    public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
} 