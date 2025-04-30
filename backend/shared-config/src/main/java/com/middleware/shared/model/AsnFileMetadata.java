package com.middleware.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "asn_file_metadata")
@Getter
@Setter
public class AsnFileMetadata extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asn_id", nullable = false)
    private AsnHeader asnHeader;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "compression_enabled", nullable = false)
    private boolean compressionEnabled;

    @Column(name = "compression_ratio")
    private Double compressionRatio;

    @Column(name = "stored_at", nullable = false)
    private LocalDateTime storedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "encryption_enabled", nullable = false)
    private boolean encryptionEnabled = false;

    @Column(name = "encryption_algorithm", length = 50)
    private String encryptionAlgorithm;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_latest_version", nullable = false)
    private boolean isLatestVersion = true;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
} 