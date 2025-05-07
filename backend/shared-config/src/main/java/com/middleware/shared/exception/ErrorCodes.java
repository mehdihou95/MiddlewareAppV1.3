package com.middleware.shared.exception;

public final class ErrorCodes {
    // Authentication errors (AUTH_*)
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
    public static final String AUTH_EXPIRED_TOKEN = "AUTH_002";
    public static final String AUTH_INVALID_TOKEN = "AUTH_003";
    public static final String AUTH_INSUFFICIENT_PRIVILEGES = "AUTH_004";
    
    // Validation errors (VAL_*)
    public static final String VAL_MISSING_FIELD = "VAL_001";
    public static final String VAL_INVALID_FORMAT = "VAL_002";
    public static final String VAL_BUSINESS_RULE = "VAL_003";
    
    // Business errors (BUS_*)
    public static final String BUS_RESOURCE_NOT_FOUND = "BUS_001";
    public static final String BUS_DUPLICATE_RESOURCE = "BUS_002";
    public static final String BUS_OPERATION_NOT_ALLOWED = "BUS_003";
    
    // System errors (SYS_*)
    public static final String SYS_UNEXPECTED_ERROR = "SYS_001";
    public static final String SYS_SERVICE_UNAVAILABLE = "SYS_002";
    public static final String SYS_DATABASE_ERROR = "SYS_003";
    
    private ErrorCodes() {
        // Prevent instantiation
    }
} 
