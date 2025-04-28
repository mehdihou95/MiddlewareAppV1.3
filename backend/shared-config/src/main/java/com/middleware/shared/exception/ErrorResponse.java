package com.middleware.shared.exception;

/**
 * Standard error response format for the application.
 */
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;

    /**
     * Constructs a new ErrorResponse with the specified details.
     *
     * @param status The HTTP status code
     * @param error The error type
     * @param message The error message
     */
    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return The status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the error type.
     *
     * @return The error type
     */
    public String getError() {
        return error;
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
