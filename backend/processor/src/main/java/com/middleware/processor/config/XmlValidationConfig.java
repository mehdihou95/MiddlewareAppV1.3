package com.middleware.processor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "xml.validation")
@Getter
@Setter
public class XmlValidationConfig {
    /**
     * Entity expansion limit for XML parsing.
     * Set to 0 for unlimited, or a positive number for a specific limit.
     */
    private String entityExpansionLimit = "5000";

    /**
     * Whether to honor all schema locations.
     */
    private boolean honourAllSchemaLocations = true;

    /**
     * Whether to enable external DTD loading.
     */
    private boolean enableExternalDtd = false;

    /**
     * Whether to enable external schema loading.
     */
    private boolean enableExternalSchema = false;

    /**
     * Whether to enable schema full checking.
     */
    private boolean enableSchemaFullChecking = true;

    /**
     * Maximum memory size for XML parsing (in bytes).
     */
    private long maxMemorySize = 1024 * 1024 * 10; // 10MB default

    /**
     * Additional XML validation features.
     * Map of feature URIs to their boolean values.
     * This allows configuring any XML parser feature without code changes.
     */
    private Map<String, Boolean> additionalFeatures = new HashMap<>();
} 
