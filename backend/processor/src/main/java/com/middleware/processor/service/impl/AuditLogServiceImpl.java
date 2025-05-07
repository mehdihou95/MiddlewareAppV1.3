package com.middleware.processor.service.impl;

import com.middleware.shared.model.AuditLog;
import com.middleware.shared.model.AuditLogEntry;
import com.middleware.shared.repository.AuditLogRepository;
import com.middleware.shared.repository.AuditLogEntryRepository;
import com.middleware.processor.service.interfaces.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;

    @Override
    @Transactional
    public AuditLogEntry saveAuditLog(AuditLogEntry auditLogEntry) {
        return auditLogEntryRepository.save(auditLogEntry);
    }

    @Override
    @Transactional
    public AuditLog createAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Override
    public AuditLog getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteAuditLog(Long id) {
        auditLogRepository.deleteById(id);
    }

    @Override
    public Page<AuditLog> getAuditLogsByClient(Long clientId, Pageable pageable) {
        return auditLogRepository.findByClientId(clientId, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByClientId(Long clientId, Pageable pageable) {
        return auditLogRepository.findByClientId(clientId, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByDateRange(String startDate, String endDate, Pageable pageable) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        return getAuditLogsByDateRange(start, end, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByUsernameAndDateRange(String username, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByUsernameAndCreatedAtBetween(username, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByClientIdAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByClientIdAndCreatedAtBetween(clientId, startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByResponseStatus(Integer status, Pageable pageable) {
        return auditLogRepository.findByResponseStatus(status, pageable);
    }

    @Override
    @Transactional
    public void deleteAuditLogsOlderThan(LocalDateTime date) {
        auditLogRepository.deleteByCreatedAtBefore(date);
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    public void cleanupOldAuditLogs() {
        // Delete audit logs older than 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        deleteAuditLogsOlderThan(thirtyDaysAgo);
    }
} 
