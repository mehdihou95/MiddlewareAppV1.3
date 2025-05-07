package com.middleware.shared.exception;

public class AuthenticationFailedException extends ApplicationException {
    public AuthenticationFailedException(String message) {
        super(ErrorCodes.AUTH_INVALID_CREDENTIALS, message);
    }
} 
