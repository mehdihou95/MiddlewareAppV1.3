package com.middleware.shared.repository;

import com.middleware.shared.model.AsnHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AsnHeader entities with circuit breaker support.
 */
@Repository
public interface AsnHeaderRepository extends BaseRepository<AsnHeader> {
    
    /**
     * Find ASN by ID and client ID
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.id = :asnId AND h.client.id = :clientId")
    Optional<AsnHeader> findByIdAndClient_Id(
        @Param("asnId") Long asnId,
        @Param("clientId") Long clientId);
    
    /**
     * Find ASN headers by client ID with pagination
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.client.id = :clientId")
    Page<AsnHeader> findByClient_Id(
        @Param("clientId") Long clientId,
        Pageable pageable);
    
    /**
     * Find ASN header by ASN number and client ID
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.asnNumber = :asnNumber AND h.client.id = :clientId")
    Optional<AsnHeader> findByAsnNumberAndClient_Id(
        @Param("asnNumber") String asnNumber,
        @Param("clientId") Long clientId);
    
    /**
     * Find ASN headers by client ID and receipt date range with pagination
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.client.id = :clientId AND h.receiptDttm BETWEEN :startDate AND :endDate")
    Page<AsnHeader> findByClient_IdAndReceiptDttmBetween(
        @Param("clientId") Long clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable);
    
    /**
     * Find ASN headers by client ID and receipt date range
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.client.id = :clientId AND h.receiptDttm BETWEEN :startDate AND :endDate")
    List<AsnHeader> findAllByClient_IdAndReceiptDttmBetween(
        @Param("clientId") Long clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find latest ASN headers by client ID
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.client.id = :clientId ORDER BY h.createdAt DESC")
    List<AsnHeader> findLatestHeaders(@Param("clientId") Long clientId);

    /**
     * Find ASN headers by client ID and receipt date
     */
    @Query("SELECT h FROM AsnHeader h WHERE h.client.id = :clientId AND h.receiptDttm = :receiptDttm")
    List<AsnHeader> findByClient_IdAndReceiptDttm(
        @Param("clientId") Long clientId,
        @Param("receiptDttm") String receiptDttm);

    /**
     * Find ASN headers by client ID and status
     */
    List<AsnHeader> findByClient_IdAndStatus(Long clientId, String status);

    /**
     * Find ASN headers by client ID and status with pagination
     */
    Page<AsnHeader> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find ASN headers by status with pagination
     */
    Page<AsnHeader> findByStatus(String status, Pageable pageable);

    /**
     * Find ASN header by ASN number
     */
    Optional<AsnHeader> findByAsnNumber(String asnNumber);
}