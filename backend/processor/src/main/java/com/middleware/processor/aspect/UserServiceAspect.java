package com.middleware.processor.aspect;

import com.middleware.shared.model.AuditLog;
import com.middleware.shared.model.User;
import com.middleware.shared.repository.AuditLogRepository;
import com.middleware.shared.repository.UserRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Aspect
@Component
public class UserServiceAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Around("execution(* com.middleware.processor.service.impl.UserServiceImpl.createUser(..))")
    public Object logCreateUser(ProceedingJoinPoint joinPoint) throws Throwable {
        User user = (User) joinPoint.getArgs()[0];
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("CREATE");
        log.setEntityType("USER");
        log.setDetails("Creating user: " + user.getUsername() + 
                      " with roles: " + String.join(", ", user.getRoles()));
        
        try {
            User createdUser = (User) joinPoint.proceed();
            log.setEntityId(createdUser.getId());
            log.setDetails("User created successfully: " + createdUser.getUsername());
            auditLogRepository.save(log);
            return createdUser;
        } catch (Throwable e) {
            log.setErrorMessage(e.getMessage());
            auditLogRepository.save(log);
            throw e;
        }
    }
    
    @Around("execution(* com.middleware.processor.service.impl.UserServiceImpl.updateUser(..))")
    public Object logUpdateUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = (Long) joinPoint.getArgs()[0];
        User userDetails = (User) joinPoint.getArgs()[1];
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("UPDATE");
        log.setEntityType("USER");
        log.setEntityId(id);
        log.setDetails("Updating user: " + userDetails.getUsername() + 
                      " with roles: " + String.join(", ", userDetails.getRoles()));
        
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Optional && ((Optional<?>) result).isPresent()) {
                User updatedUser = (User) ((Optional<?>) result).get();
                log.setDetails("User updated successfully: " + updatedUser.getUsername());
            }
            auditLogRepository.save(log);
            return result;
        } catch (Throwable e) {
            log.setErrorMessage(e.getMessage());
            auditLogRepository.save(log);
            throw e;
        }
    }
    
    @Around("execution(* com.middleware.processor.service.impl.UserServiceImpl.deleteUser(..))")
    public Object logDeleteUser(ProceedingJoinPoint joinPoint) throws Throwable {
        Long userId = (Long) joinPoint.getArgs()[0];
        
        // Get the user details before deletion
        String username = userRepository.findById(userId)
            .map(User::getUsername)
            .orElse("Unknown");
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("DELETE");
        log.setEntityType("USER");
        log.setEntityId(userId);
        log.setDetails("User deleted: " + username);
        
        try {
            Object result = joinPoint.proceed();
            auditLogRepository.save(log);
            return result;
        } catch (Throwable e) {
            log.setErrorMessage(e.getMessage());
            auditLogRepository.save(log);
            throw e;
        }
    }
    
    @Around("execution(* com.middleware.processor.service.impl.UserServiceImpl.changePassword(..))")
    public Object logPasswordChange(ProceedingJoinPoint joinPoint) throws Throwable {
        Long userId = (Long) joinPoint.getArgs()[0];
        
        String username = userRepository.findById(userId)
            .map(User::getUsername)
            .orElse("Unknown");
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("PASSWORD_CHANGE");
        log.setEntityType("USER");
        log.setEntityId(userId);
        log.setDetails("Password change attempted for user: " + username);
        
        try {
            boolean success = (boolean) joinPoint.proceed();
            log.setDetails("Password " + (success ? "changed successfully" : "change failed") + 
                         " for user: " + username);
            auditLogRepository.save(log);
            return success;
        } catch (Throwable e) {
            log.setErrorMessage(e.getMessage());
            auditLogRepository.save(log);
            throw e;
        }
    }
    
    @Around("execution(* com.middleware.processor.service.impl.UserServiceImpl.resetPassword(..))")
    public Object logPasswordReset(ProceedingJoinPoint joinPoint) throws Throwable {
        Long userId = (Long) joinPoint.getArgs()[0];
        
        String username = userRepository.findById(userId)
            .map(User::getUsername)
            .orElse("Unknown");
        
        AuditLog log = new AuditLog();
        log.setUsername(getCurrentUsername());
        log.setAction("PASSWORD_RESET");
        log.setEntityType("USER");
        log.setEntityId(userId);
        log.setDetails("Password reset initiated for user: " + username);
        
        try {
            Object result = joinPoint.proceed();
            log.setDetails("Password reset successful for user: " + username);
            auditLogRepository.save(log);
            return result;
        } catch (Throwable e) {
            log.setErrorMessage(e.getMessage());
            auditLogRepository.save(log);
            throw e;
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
} 
