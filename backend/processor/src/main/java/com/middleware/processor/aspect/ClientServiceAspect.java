package com.middleware.processor.aspect;

import com.middleware.shared.model.AuditLog;
import com.middleware.shared.model.Client;
import com.middleware.shared.repository.AuditLogRepository;
import com.middleware.shared.repository.ClientRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ClientServiceAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private ClientRepository clientRepository;
    
    @Around("execution(* com.middleware.processor.service.impl.ClientServiceImpl.saveClient(..))")
    public Object logClientSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Client client = (Client) joinPoint.getArgs()[0];
        String action = client.getId() == null ? "CREATE" : "UPDATE";
        
        // First proceed with saving the client
        Client savedClient = (Client) joinPoint.proceed();
        
        // Now create the audit log with the saved client's ID
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction(action);
        log.setEntityType("CLIENT");
        log.setEntityId(savedClient.getId());
        log.setDetails("Client " + action.toLowerCase() + "d: " + savedClient.getName());
        
        auditLogRepository.save(log);
        
        return savedClient;
    }
    
    @Around("execution(* com.middleware.processor.service.impl.ClientServiceImpl.deleteClient(..))")
    public Object logClientDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Long clientId = (Long) joinPoint.getArgs()[0];
        
        // Get the client name before deletion
        String clientName = clientRepository.findById(clientId)
            .map(Client::getName)
            .orElse("Unknown");
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("DELETE");
        log.setEntityType("CLIENT");
        log.setEntityId(clientId);
        log.setDetails("Client deleted: " + clientName);
        
        auditLogRepository.save(log);
        
        return joinPoint.proceed();
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
} 
