package com.middleware.shared.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Data
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;
    private List<String> details;
    private Map<String, String> fieldErrors;
    
    public ErrorResponse(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.fieldErrors = new HashMap<>();
    }

    public ErrorResponse(int status, String code, String message, LocalDateTime timestamp) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.fieldErrors = new HashMap<>();
    }

    public ErrorResponse(int status, String code, String message, LocalDateTime timestamp, List<String> errors) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
        this.fieldErrors = new HashMap<>();
    }

    public ErrorResponse(int status, String code, String message, LocalDateTime timestamp, List<String> errors, List<String> details) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
        this.details = details;
        this.fieldErrors = new HashMap<>();
    }

    public ErrorResponse(String code, String message) {
        this.status = 400;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.fieldErrors = new HashMap<>();
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public void addFieldError(String field, String message) {
        this.fieldErrors.put(field, message);
    }
} 
