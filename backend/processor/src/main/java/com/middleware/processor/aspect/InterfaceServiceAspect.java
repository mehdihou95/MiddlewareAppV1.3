package com.middleware.processor.aspect;

import com.middleware.shared.model.AuditLog;
import com.middleware.shared.model.Interface;
import com.middleware.shared.repository.AuditLogRepository;
import com.middleware.shared.repository.InterfaceRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class InterfaceServiceAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private InterfaceRepository interfaceRepository;
    
    @Around("execution(* com.middleware.processor.service.impl.InterfaceServiceImpl.createInterface(..))")
    public Object logCreateInterface(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Interface) {
            Interface interfaceEntity = (Interface) args[0];
            
            AuditLog log = new AuditLog();
            log.setUsername(getCurrentUsername());
            log.setAction("CREATE");
            log.setEntityType("INTERFACE");
            log.setDetails("Creating interface: " + interfaceEntity.getName() + 
                          " (type: " + interfaceEntity.getType() + ") " +
                          "for client: " + interfaceEntity.getClient().getId());
            
            if (interfaceEntity.getClient() != null) {
                log.setClientId(interfaceEntity.getClient().getId());
            }
            
            try {
                Object result = joinPoint.proceed();
                if (result instanceof Interface) {
                    Interface createdInterface = (Interface) result;
                    log.setEntityId(createdInterface.getId());
                    log.setDetails("Interface created successfully: " + createdInterface.getName());
                }
                auditLogRepository.save(log);
                return result;
            } catch (Throwable e) {
                log.setErrorMessage(e.getMessage());
                auditLogRepository.save(log);
                throw e;
            }
        }
        return joinPoint.proceed();
    }
    
    @Around("execution(* com.middleware.processor.service.impl.InterfaceServiceImpl.updateInterface(..))")
    public Object logUpdateInterface(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length > 1 && args[1] instanceof Interface) {
            Long id = (Long) args[0];
            Interface interfaceEntity = (Interface) args[1];
            
            AuditLog log = new AuditLog();
            log.setUsername(getCurrentUsername());
            log.setAction("UPDATE");
            log.setEntityType("INTERFACE");
            log.setEntityId(id);
            
            if (interfaceEntity.getClient() != null) {
                log.setClientId(interfaceEntity.getClient().getId());
                log.setDetails("Updating interface: " + interfaceEntity.getName() + 
                             " (type: " + interfaceEntity.getType() + ") " +
                             "for client: " + interfaceEntity.getClient().getId());
            }
            
            try {
                Object result = joinPoint.proceed();
                if (result instanceof Interface) {
                    Interface updatedInterface = (Interface) result;
                    log.setDetails("Interface updated successfully: " + updatedInterface.getName());
                }
                auditLogRepository.save(log);
                return result;
            } catch (Throwable e) {
                log.setErrorMessage(e.getMessage());
                auditLogRepository.save(log);
                throw e;
            }
        }
        return joinPoint.proceed();
    }
    
    @Before("execution(* com.middleware.processor.service.impl.InterfaceServiceImpl.deleteInterface(..))")
    public void logDeleteInterface(JoinPoint joinPoint) {
        Long interfaceId = (Long) joinPoint.getArgs()[0];
        
        // Get the interface details before deletion
        interfaceRepository.findById(interfaceId).ifPresent(interfaceEntity -> {
            AuditLog log = new AuditLog();
            log.setUsername(getCurrentUsername());
            log.setAction("DELETE");
            log.setEntityType("INTERFACE");
            log.setEntityId(interfaceId);
            log.setClientId(interfaceEntity.getClient().getId());
            log.setDetails("Interface deleted: " + interfaceEntity.getName() + " for client: " + interfaceEntity.getClient().getName());
            
            auditLogRepository.save(log);
        });
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
} 
