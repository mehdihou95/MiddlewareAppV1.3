package com.middleware.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard error response format for the application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String code;
    private long timestamp = System.currentTimeMillis();

    /**
     * Constructs a new ErrorResponse with the specified details.
     *
     * @param status The HTTP status code
     * @param error The error type
     * @param message The error message
     */
    public ErrorResponse(int status, String error, String message) {
        this.message = message;
        this.code = error;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return The status code
     */
    public int getStatus() {
        return Integer.parseInt(code);
    }

    /**
     * Gets the error type.
     *
     * @return The error type
     */
    public String getError() {
        return code;
    }

    /**
     * Gets the error message.
     *
     * @return The error message
     */
    public String getMessage() {
        return message;
    }
} 
