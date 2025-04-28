package com.middleware.shared.exception;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Base exception class for validation failures across all modules.
 */
public class BaseValidationException extends RuntimeException {
    
    private final Map<String, String> fieldErrors;
    
    /**
     * Constructs a new BaseValidationException with the specified message.
     *
     * @param message The error message
     */
    public BaseValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructs a new BaseValidationException with the specified message and cause.
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public BaseValidationException(String message, Throwable cause) {
        super(message, cause);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructs a new BaseValidationException with the specified message and field errors.
     *
     * @param message The error message
     * @param fieldErrors Map of field names to error messages
     */
    public BaseValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
    }
    
    /**
     * Gets the field-level validation errors.
     *
     * @return Map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
    
    /**
     * Add a field error.
     *
     * @param field The field name
     * @param error The error message
     */
    public void addFieldError(String field, String error) {
        fieldErrors.put(field, error);
    }
} 