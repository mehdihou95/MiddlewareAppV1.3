package com.middleware.processor.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class XmlValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public XmlValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }

    public XmlValidationException(List<String> validationErrors) {
        super(String.join(", ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public XmlValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = List.of(message);
    }
} 
