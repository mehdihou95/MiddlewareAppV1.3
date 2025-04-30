package com.middleware.shared.repository;

import com.middleware.shared.model.AsnFileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ASN file metadata
 */
@Repository
public interface AsnFileMetadataRepository extends BaseRepository<AsnFileMetadata> {
    
    /**
     * Find metadata by ASN header ID
     */
    Optional<AsnFileMetadata> findByAsnHeader_Id(Long asnId);
    
    /**
     * Find metadata by file hash
     */
    Optional<AsnFileMetadata> findByFileHash(String fileHash);
    
    /**
     * Find metadata by ASN number and client ID
     */
    Optional<AsnFileMetadata> findByAsnHeader_AsnNumberAndAsnHeader_Client_Id(String asnNumber, Long clientId);
    
    /**
     * Find all metadata for a client with pagination
     */
    Page<AsnFileMetadata> findByAsnHeader_Client_Id(Long clientId, Pageable pageable);
    
    /**
     * Find all metadata for a client
     */
    List<AsnFileMetadata> findByAsnHeader_Client_Id(Long clientId);
    
    /**
     * Find metadata by storage date range with pagination
     */
    Page<AsnFileMetadata> findByStoredAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find metadata by last access date range with pagination
     */
    Page<AsnFileMetadata> findByLastAccessedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find metadata by file size range with pagination
     */
    Page<AsnFileMetadata> findByFileSizeBetween(Long minSize, Long maxSize, Pageable pageable);
    
    /**
     * Find metadata by compression status with pagination
     */
    Page<AsnFileMetadata> findByCompressionEnabled(boolean compressionEnabled, Pageable pageable);
    
    /**
     * Find metadata by encryption status with pagination
     */
    Page<AsnFileMetadata> findByEncryptionEnabled(boolean encryptionEnabled, Pageable pageable);
    
    /**
     * Find metadata by version with pagination
     */
    Page<AsnFileMetadata> findByVersion(Integer version, Pageable pageable);
    
    /**
     * Find latest version metadata for an ASN
     */
    Optional<AsnFileMetadata> findByAsnHeader_IdAndIsLatestVersionTrue(Long asnId);
    
    /**
     * Find all versions of metadata for an ASN
     */
    List<AsnFileMetadata> findByAsnHeader_IdOrderByVersionDesc(Long asnId);
    
    /**
     * Find metadata by checksum
     */
    Optional<AsnFileMetadata> findByChecksum(String checksum);
    
    /**
     * Find metadata by access count range with pagination
     */
    Page<AsnFileMetadata> findByAccessCountBetween(Integer minCount, Integer maxCount, Pageable pageable);
} 
