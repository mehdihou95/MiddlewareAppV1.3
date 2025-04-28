package com.middleware.processor.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Utility class for transaction management.
 * Centralizes transaction handling to reduce duplication across services.
 */
@Component
public class TransactionUtils {
    private static final Logger log = LoggerFactory.getLogger(TransactionUtils.class);
    
    private final PlatformTransactionManager transactionManager;
    
    @Autowired
    public TransactionUtils(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    
    /**
     * Execute an operation within a transaction with specified propagation and isolation
     * 
     * @param propagationBehavior Transaction propagation behavior
     * @param isolationLevel Transaction isolation level
     * @param operation The operation to execute within the transaction
     * @return The result of the operation
     */
    public <T> T executeInTransaction(int propagationBehavior, int isolationLevel, Supplier<T> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(propagationBehavior);
        transactionTemplate.setIsolationLevel(isolationLevel);
        
        return transactionTemplate.execute(status -> {
            try {
                return operation.get();
            } catch (Exception e) {
                log.error("Transaction error: ", e);
                throw e;
            }
        });
    }
    
    /**
     * Execute an operation in a new transaction
     * 
     * @param operation The operation to execute within a new transaction
     * @return The result of the operation
     */
    public <T> T executeInNewTransaction(Supplier<T> operation) {
        return executeInTransaction(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            TransactionDefinition.ISOLATION_READ_COMMITTED,
            operation
        );
    }
    
    /**
     * Execute an operation in a read-only transaction
     * 
     * @param operation The operation to execute within a read-only transaction
     * @return The result of the operation
     */
    public <T> T executeInReadOnlyTransaction(Supplier<T> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setReadOnly(true);
        
        return transactionTemplate.execute(status -> {
            try {
                return operation.get();
            } catch (Exception e) {
                log.error("Read-only transaction error: ", e);
                throw e;
            }
        });
    }
}
