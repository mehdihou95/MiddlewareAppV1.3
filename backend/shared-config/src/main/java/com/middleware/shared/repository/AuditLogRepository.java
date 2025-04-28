package com.middleware.shared.repository;

import com.middleware.shared.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository interface for AuditLog entities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    Page<AuditLog> findByClientId(Long clientId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<AuditLog> findByUsernameAndCreatedAtBetween(String username, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<AuditLog> findByClientIdAndCreatedAtBetween(Long clientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<AuditLog> findByResponseStatus(Integer status, Pageable pageable);
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :date")
    void deleteByCreatedAtBefore(@Param("date") LocalDateTime date);
} 
