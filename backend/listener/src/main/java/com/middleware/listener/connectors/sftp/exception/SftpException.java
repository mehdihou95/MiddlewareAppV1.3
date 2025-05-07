package com.middleware.listener.connectors.sftp.exception;

public class SftpException extends RuntimeException {
    
    public SftpException(String message) {
        super(message);
    }
    
    public SftpException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static class AuthenticationException extends SftpException {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ConnectionException extends SftpException {
        public ConnectionException(String message) {
            super(message);
        }

        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class TransferException extends SftpException {
        public TransferException(String message) {
            super(message);
        }

        public TransferException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 