package com.middleware.processor.exception;

import com.middleware.shared.exception.BaseValidationException;
import java.util.Map;
import java.util.HashMap;

/**
 * Processor-specific validation exception.
 * Extends BaseValidationException to provide additional functionality specific to the processor module.
 */
public class ValidationException extends RuntimeException {
    
    private final Map<String, String> fieldErrors = new HashMap<>();
    
    /**
     * Constructs a new ValidationException with the specified message.
     *
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ValidationException with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ValidationException with the specified message and field errors.
     *
     * @param message The error message
     * @param fieldErrors Map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors.putAll(fieldErrors);
    }
    
    /**
     * Add a field error to the exception.
     *
     * @param fieldName The name of the field with the error
     * @param errorMessage The error message for the field
     */
    public void addFieldError(String fieldName, String errorMessage) {
        fieldErrors.put(fieldName, errorMessage);
    }
    
    /**
     * Get all field errors.
     *
     * @return Map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return new HashMap<>(fieldErrors);
    }
    
    /**
     * Create a validation exception for XML parsing errors.
     *
     * @param message The error message
     * @param cause The cause of the error
     * @return A new ValidationException
     */
    public static ValidationException xmlParsingError(String message, Throwable cause) {
        return new ValidationException("XML Parsing Error: " + message, cause);
    }
    
    /**
     * Create a validation exception for XML schema validation errors.
     *
     * @param message The error message
     * @return A new ValidationException
     */
    public static ValidationException schemaValidationError(String message) {
        return new ValidationException("Schema Validation Error: " + message);
    }
    
    /**
     * Create a validation exception for missing required fields.
     *
     * @param fieldName The name of the missing field
     * @return A new ValidationException
     */
    public static ValidationException missingRequiredField(String fieldName) {
        ValidationException ex = new ValidationException("Missing required field: " + fieldName);
        ex.addFieldError(fieldName, "Field is required");
        return ex;
    }
} 
