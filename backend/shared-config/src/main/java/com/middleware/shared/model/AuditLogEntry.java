package com.middleware.shared.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "method_audit_logs")
public class AuditLogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String message;

    @Column
    private String error;

    @Column(nullable = false)
    private long duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditLogLevel level;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;
} 