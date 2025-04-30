package com.middleware.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for ASN file storage
 */
@Configuration
@ConfigurationProperties(prefix = "asn.file.storage")
@Getter
@Setter
public class FileStorageConfig {
    
    /**
     * Base path for storing ASN files
     */
    private String basePath = "/var/data/asn/files";
    
    /**
     * Number of days to retain files
     */
    private int retentionDays = 90;
    
    /**
     * Cron expression for cleanup job
     */
    private String cleanupCron = "0 0 2 * * ?";
    
    /**
     * Maximum file size in bytes
     */
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    
    /**
     * Allowed file extensions
     */
    private String[] allowedExtensions = {"xml"};
    
    /**
     * Compression enabled
     */
    private boolean compressionEnabled = true;
    
    /**
     * Compression level (0-9)
     */
    private int compressionLevel = 6;
} 