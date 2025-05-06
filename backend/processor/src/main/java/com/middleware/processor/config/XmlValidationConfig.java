package com.middleware.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "xml.validation")
public class XmlValidationConfig {
    private int entityExpansionLimit = 1000;
    private boolean secureProcessing = true;
    private boolean enableExternalDtd = false;
    private boolean enableExternalSchema = false;
    private boolean honourAllSchemaLocations = true;
    private boolean enableSchemaFullChecking = true;
    private Map<String, Boolean> additionalFeatures;
    private String schemaBasePath = "src/main/resources/xsd";
    private String defaultSchemaPath = "order_default_namespace.xsd";

    @Bean
    @Primary
    public SchemaFactory schemaFactory() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        
        // Set security features
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, secureProcessing);
        factory.setProperty("http://apache.org/xml/properties/security-manager", null);
        
        // Set entity expansion limit
        factory.setProperty("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", entityExpansionLimit);
        
        // Set external resource loading
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, enableExternalDtd ? "all" : "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, enableExternalSchema ? "all" : "");
        
        // Set validation features
        factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", honourAllSchemaLocations);
        factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", enableSchemaFullChecking);
        
        // Set additional features if provided
        if (additionalFeatures != null) {
            additionalFeatures.forEach((key, value) -> {
                try {
                    factory.setFeature(key, value);
                } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                    throw new RuntimeException("Failed to set XML validation feature: " + key, e);
                }
            });
        }
        
        return factory;
    }

    @Bean
    @Primary
    public Schema defaultSchema(SchemaFactory schemaFactory) throws SAXException {
        try {
            File schemaFile = new File(schemaBasePath, defaultSchemaPath);
            if (!schemaFile.exists()) {
                throw new IllegalStateException("Default schema file not found: " + schemaFile.getAbsolutePath());
            }
            return schemaFactory.newSchema(schemaFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default schema", e);
        }
    }

    // Getters and Setters
    public int getEntityExpansionLimit() {
        return entityExpansionLimit;
    }

    public void setEntityExpansionLimit(int entityExpansionLimit) {
        this.entityExpansionLimit = entityExpansionLimit;
    }

    public boolean isSecureProcessing() {
        return secureProcessing;
    }

    public void setSecureProcessing(boolean secureProcessing) {
        this.secureProcessing = secureProcessing;
    }

    public boolean isEnableExternalDtd() {
        return enableExternalDtd;
    }

    public void setEnableExternalDtd(boolean enableExternalDtd) {
        this.enableExternalDtd = enableExternalDtd;
    }

    public boolean isEnableExternalSchema() {
        return enableExternalSchema;
    }

    public void setEnableExternalSchema(boolean enableExternalSchema) {
        this.enableExternalSchema = enableExternalSchema;
    }

    public boolean isHonourAllSchemaLocations() {
        return honourAllSchemaLocations;
    }

    public void setHonourAllSchemaLocations(boolean honourAllSchemaLocations) {
        this.honourAllSchemaLocations = honourAllSchemaLocations;
    }

    public boolean isEnableSchemaFullChecking() {
        return enableSchemaFullChecking;
    }

    public void setEnableSchemaFullChecking(boolean enableSchemaFullChecking) {
        this.enableSchemaFullChecking = enableSchemaFullChecking;
    }

    public Map<String, Boolean> getAdditionalFeatures() {
        return additionalFeatures;
    }

    public void setAdditionalFeatures(Map<String, Boolean> additionalFeatures) {
        this.additionalFeatures = additionalFeatures;
    }

    public String getSchemaBasePath() {
        return schemaBasePath;
    }

    public void setSchemaBasePath(String schemaBasePath) {
        this.schemaBasePath = schemaBasePath;
    }

    public String getDefaultSchemaPath() {
        return defaultSchemaPath;
    }

    public void setDefaultSchemaPath(String defaultSchemaPath) {
        this.defaultSchemaPath = defaultSchemaPath;
    }
} 
