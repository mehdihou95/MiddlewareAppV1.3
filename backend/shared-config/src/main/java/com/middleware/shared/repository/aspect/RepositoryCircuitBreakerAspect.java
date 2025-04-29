package com.middleware.shared.repository.aspect;

import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import com.middleware.shared.exception.BaseValidationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Aspect for applying circuit breaker pattern to repository operations.
 * Intercepts repository method calls and applies circuit breaker protection.
 */
@Aspect
@Component
public class RepositoryCircuitBreakerAspect {

    private static final Logger log = LoggerFactory.getLogger(RepositoryCircuitBreakerAspect.class);
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;
    
    /**
     * Pointcut for all repository methods.
     */
    @Pointcut("execution(* com.middleware.shared.repository.*Repository.*(..))")
    public void repositoryMethods() {}
    
    /**
     * Apply circuit breaker pattern to repository methods.
     * 
     * @param joinPoint The join point
     * @return The result of the repository method call
     * @throws Throwable If an error occurs
     */
    @Around("repositoryMethods()")
    public Object applyCircuitBreaker(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Sanitize and log method information
        log.debug("Applying circuit breaker to repository method: {}.{}", className, methodName);
        log.debug("Method arguments count: {}", joinPoint.getArgs().length);
        
        // Sanitize sensitive data in arguments
        Object[] sanitizedArgs = Arrays.stream(joinPoint.getArgs())
            .map(arg -> {
                if (arg == null) return "null";
                if (arg instanceof ProcessedFile) {
                    return String.format("ProcessedFile[id=%s]", ((ProcessedFile) arg).getId());
                }
                if (arg instanceof Client) {
                    return String.format("Client[id=%s]", ((Client) arg).getId());
                }
                if (arg instanceof Interface) {
                    return String.format("Interface[id=%s]", ((Interface) arg).getId());
                }
                return arg.getClass().getSimpleName();
            })
            .toArray();
            
        log.debug("Sanitized arguments: {}", Arrays.toString(sanitizedArgs));
        
        try {
            // Use the existing session instead of creating new ones
            return circuitBreakerService.executeRepositoryOperation(
                () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable t) {
                        log.error("Error executing repository method {}.{}: {}", className, methodName, t.getMessage(), t);
                        throw wrapException(t);
                    }
                },
                () -> {
                    log.warn("Circuit breaker fallback for method {}.{}", className, methodName);
                    return getFallbackValue(method.getReturnType(), joinPoint.getArgs());
                }
            );
        } catch (Exception e) {
            log.error("Circuit breaker error in {}.{}: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Handle find methods with appropriate fallbacks based on return type.
     */
    private Object handleFindMethod(ProceedingJoinPoint joinPoint, String methodName, Class<?> returnType) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    log.error("Error executing repository find method {}.{}: {}", className, methodName, t.getMessage(), t);
                    throw wrapException(t);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback for find method {}.{}", className, methodName);
                return getFallbackValue(returnType, joinPoint.getArgs());
            }
        );
    }
    
    /**
     * Handle save methods with appropriate fallbacks.
     */
    private Object handleSaveMethod(ProceedingJoinPoint joinPoint, Class<?> returnType) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    log.error("Error executing repository save method {}.{}: {}", className, methodName, t.getMessage(), t);
                    throw wrapException(t);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback for save method {}.{}", className, methodName);
                // Return the original entity as fallback
                if (joinPoint.getArgs().length > 0) {
                    return joinPoint.getArgs()[0];
                }
                return null;
            }
        );
    }
    
    /**
     * Handle delete methods with appropriate fallbacks.
     */
    private Object handleDeleteMethod(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    joinPoint.proceed();
                } catch (Throwable t) {
                    log.error("Error executing repository delete method {}.{}: {}", className, methodName, t.getMessage(), t);
                    throw wrapException(t);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback for delete method {}.{}", className, methodName);
                // No action needed for fallback
            }
        );
        return null;
    }
    
    /**
     * Handle count and exists methods with appropriate fallbacks.
     */
    private Object handleCountMethod(ProceedingJoinPoint joinPoint, String methodName) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    log.error("Error executing repository count method {}.{}: {}", className, methodName, t.getMessage(), t);
                    throw wrapException(t);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback for count method {}.{}", className, methodName);
                // Return 0 for count, false for exists
                return methodName.equals("count") ? 0L : false;
            }
        );
    }

    /**
     * Handle default method with appropriate fallbacks.
     */
    private Object handleDefaultMethod(ProceedingJoinPoint joinPoint, String className, String methodName, Class<?> returnType) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable t) {
                    log.error("Error executing repository method {}.{}: {}", className, methodName, t.getMessage(), t);
                    throw wrapException(t);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback for method {}.{}", className, methodName);
                return getFallbackValue(returnType, joinPoint.getArgs());
            }
        );
    }

    /**
     * Get appropriate fallback value based on return type.
     */
    private Object getFallbackValue(Class<?> returnType, Object[] args) {
        if (Page.class.isAssignableFrom(returnType)) {
            // Find Pageable argument if exists
            Pageable pageable = null;
            for (Object arg : args) {
                if (arg instanceof Pageable) {
                    pageable = (Pageable) arg;
                    break;
                }
            }
            return Page.empty(pageable != null ? pageable : Pageable.unpaged());
        } else if (Optional.class.isAssignableFrom(returnType)) {
            return Optional.empty();
        } else if (Iterable.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        } else if (returnType.isPrimitive()) {
            if (returnType == boolean.class) return false;
            if (returnType == long.class) return 0L;
            if (returnType == int.class) return 0;
            if (returnType == double.class) return 0.0;
            if (returnType == float.class) return 0.0f;
            return null;
        }
        return null;
    }

    /**
     * Wrap throwable in appropriate runtime exception.
     */
    private RuntimeException wrapException(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof TimeoutException) {
            return new BaseValidationException("Operation timed out: " + t.getMessage(), t);
        }
        return new RuntimeException("Repository operation failed", t);
    }
}
