package com.middleware.processor.aspect;

import com.middleware.processor.annotation.AuditLog;
import com.middleware.shared.model.AuditLogEntry;
import com.middleware.shared.model.AuditLogLevel;
import com.middleware.processor.service.interfaces.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final AuditLogService auditLogService;

    @Around("@annotation(com.middleware.processor.annotation.AuditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            AuditLog auditLogAnnotation = signature.getMethod().getAnnotation(AuditLog.class);

            AuditLogEntry entry = new AuditLogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setUsername(authentication != null ? authentication.getName() : "anonymous");
            entry.setMethod(signature.getMethod().getName());
            entry.setDuration(duration);
            entry.setLevel(exception != null ? AuditLogLevel.ERROR : AuditLogLevel.INFO);
            entry.setMessage(auditLogAnnotation.value());
            
            if (exception != null) {
                entry.setError(exception.getMessage());
            }

            auditLogService.saveAuditLog(entry);
        }
    }
} 
