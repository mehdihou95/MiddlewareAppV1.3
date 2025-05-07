package com.middleware.processor.exception;

public class MappingRuleException extends RuntimeException {
    private final String tableName;
    private final Long clientId;

    public MappingRuleException(String message, String tableName, Long clientId) {
        super(message);
        this.tableName = tableName;
        this.clientId = clientId;
    }

    public MappingRuleException(String message, String tableName, Long clientId, Throwable cause) {
        super(message, cause);
        this.tableName = tableName;
        this.clientId = clientId;
    }

    public String getTableName() {
        return tableName;
    }

    public Long getClientId() {
        return clientId;
    }
} 
