package com.middleware.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardErrorResponse {
    private String code;           // Application-specific error code
    private String message;        // User-friendly message
    private String detail;         // Detailed explanation (optional)
    private Map<String, String> fieldErrors; // For validation errors
    private List<String> errorList;  // For multiple errors
    private LocalDateTime timestamp;
    
    // Static factory methods for common error types
    public static StandardErrorResponse validationError(String message, Map<String, String> fieldErrors) {
        return StandardErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(message)
            .fieldErrors(fieldErrors != null ? fieldErrors : new HashMap<>())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static StandardErrorResponse authenticationError(String message) {
        return StandardErrorResponse.builder()
            .code("AUTH_ERROR")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static StandardErrorResponse businessError(String code, String message) {
        return StandardErrorResponse.builder()
            .code(code)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static StandardErrorResponse systemError(String message) {
        return StandardErrorResponse.builder()
            .code("SYSTEM_ERROR")
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
} 
