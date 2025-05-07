package com.middleware.shared.exception;

public class TokenException extends ApplicationException {
    public TokenException(String message) {
        super(ErrorCodes.AUTH_INVALID_TOKEN, message);
    }
    
    public static TokenException expired() {
        return new TokenException("Token has expired");
    }
    
    public static TokenException invalid() {
        return new TokenException("Token is invalid");
    }
    
    public static TokenException blacklisted() {
        return new TokenException("Token has been revoked");
    }
} 
