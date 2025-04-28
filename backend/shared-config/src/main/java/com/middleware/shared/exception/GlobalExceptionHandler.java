package com.middleware.shared.exception;

import com.middleware.shared.model.StandardErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import io.jsonwebtoken.JwtException;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles various exceptions and returns appropriate HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<StandardErrorResponse> handleApplicationException(ApplicationException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(response);
    }
    
    @ExceptionHandler(BaseValidationException.class)
    public ResponseEntity<StandardErrorResponse> handleValidationException(BaseValidationException ex) {
        StandardErrorResponse response = StandardErrorResponse.validationError(
            "Validation failed", 
            ex.getFieldErrors()
        );
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage(),
                (existing, replacement) -> existing + "; " + replacement
            ));
        
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.VAL_INVALID_FORMAT)
            .message("Validation failed")
            .fieldErrors(fieldErrors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex) {
        
        Map<String, String> fieldErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                violation -> violation.getMessage(),
                (existing, replacement) -> existing + "; " + replacement
            ));
        
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.VAL_INVALID_FORMAT)
            .message("Validation failed")
            .fieldErrors(fieldErrors)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardErrorResponse> handleAuthenticationException(
            AuthenticationException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.AUTH_INVALID_CREDENTIALS)
            .message("Authentication failed")
            .detail(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.AUTH_INSUFFICIENT_PRIVILEGES)
            .message("Access denied")
            .detail(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.AUTH_INVALID_CREDENTIALS)
            .message("Invalid credentials")
            .detail("Username or password is incorrect")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<StandardErrorResponse> handleJwtException(JwtException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.AUTH_INVALID_TOKEN)
            .message("Invalid or expired token")
            .detail(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        
        String message = "Database constraint violation";
        Throwable cause = ex.getRootCause();
        
        // Extract specific database error information
        if (cause instanceof SQLIntegrityConstraintViolationException) {
            SQLIntegrityConstraintViolationException sqlEx = (SQLIntegrityConstraintViolationException) cause;
            if (sqlEx.getErrorCode() == 1062) { // MySQL duplicate entry
                message = "A record with this information already exists";
            } else if (sqlEx.getErrorCode() == 1452) { // MySQL foreign key constraint
                message = "Referenced record does not exist";
            }
        }
        
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.BUS_DUPLICATE_RESOURCE)
            .message(message)
            .detail(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<StandardErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.BUS_RESOURCE_NOT_FOUND)
            .message("Resource not found")
            .detail(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        
        StandardErrorResponse response = StandardErrorResponse.builder()
            .code(ErrorCodes.SYS_UNEXPECTED_ERROR)
            .message("An unexpected error occurred")
            .detail("Please try again later or contact support if the problem persists")
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode.startsWith("AUTH_")) {
            return HttpStatus.UNAUTHORIZED;
        } else if (errorCode.startsWith("VAL_")) {
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.startsWith("BUS_")) {
            if (errorCode.equals(ErrorCodes.BUS_RESOURCE_NOT_FOUND)) {
                return HttpStatus.NOT_FOUND;
            } else if (errorCode.equals(ErrorCodes.BUS_DUPLICATE_RESOURCE)) {
                return HttpStatus.CONFLICT;
            }
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.startsWith("SYS_")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
} 
