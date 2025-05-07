package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.AuditLog;
import com.middleware.shared.model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

/**
 * Service interface for audit log operations.
 */
public interface AuditLogService {
    /**
     * Creates a new audit log entry.
     *
     * @param auditLog The audit log to create
     * @return The created audit log
     */
    AuditLog createAuditLog(AuditLog auditLog);

    /**
     * Retrieves all audit logs with pagination.
     *
     * @param pageable The pagination information
     * @return A page of audit logs
     */
    Page<AuditLog> getAuditLogs(Pageable pageable);

    /**
     * Retrieves audit logs for a specific client.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return A page of audit logs for the client
     */
    Page<AuditLog> getAuditLogsByClient(Long clientId, Pageable pageable);

    /**
     * Retrieves audit logs by username with pagination.
     *
     * @param username The username to filter by
     * @param pageable The pagination information
     * @return A page of audit logs for the username
     */
    Page<AuditLog> getAuditLogsByUsername(String username, Pageable pageable);

    /**
     * Retrieves audit logs by client ID with pagination.
     *
     * @param clientId The client ID to filter by
     * @param pageable The pagination information
     * @return A page of audit logs for the client ID
     */
    Page<AuditLog> getAuditLogsByClientId(Long clientId, Pageable pageable);

    /**
     * Retrieves audit logs by action with pagination.
     *
     * @param action The action to filter by
     * @param pageable The pagination information
     * @return A page of audit logs for the action
     */
    Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable);

    /**
     * Retrieves audit logs within a date range with pagination.
     *
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @param pageable The pagination information
     * @return A page of audit logs within the date range
     */
    Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Retrieves audit logs by username within a date range with pagination.
     *
     * @param username The username to filter by
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @param pageable The pagination information
     * @return A page of audit logs for the username within the date range
     */
    Page<AuditLog> getAuditLogsByUsernameAndDateRange(String username, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Retrieves audit logs by client ID within a date range with pagination.
     *
     * @param clientId The client ID to filter by
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @param pageable The pagination information
     * @return A page of audit logs for the client ID within the date range
     */
    Page<AuditLog> getAuditLogsByClientIdAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Retrieves audit logs by response status with pagination.
     *
     * @param status The response status to filter by
     * @param pageable The pagination information
     * @return A page of audit logs for the response status
     */
    Page<AuditLog> getAuditLogsByResponseStatus(Integer status, Pageable pageable);

    /**
     * Deletes audit logs older than the specified date.
     *
     * @param date The date to delete audit logs older than
     */
    void deleteAuditLogsOlderThan(LocalDateTime date);

    /**
     * Retrieves audit logs by date range with pagination.
     *
     * @param startDate The start date of the range
     * @param endDate The end date of the range
     * @param pageable The pagination information
     * @return A page of audit logs within the date range
     */
    Page<AuditLog> getAuditLogsByDateRange(String startDate, String endDate, Pageable pageable);

    /**
     * Retrieves audit logs by ID.
     *
     * @param id The ID of the audit log to retrieve
     * @return The retrieved audit log
     */
    AuditLog getAuditLogById(Long id);

    /**
     * Deletes audit logs by ID.
     *
     * @param id The ID of the audit log to delete
     */
    void deleteAuditLog(Long id);

    /**
     * Saves an audit log entry.
     *
     * @param auditLogEntry The audit log entry to save
     * @return The saved audit log entry
     */
    AuditLogEntry saveAuditLog(AuditLogEntry auditLogEntry);
} 
