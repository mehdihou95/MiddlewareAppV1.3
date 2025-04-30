package com.middleware.shared.repository;

import com.middleware.shared.model.AsnLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AsnLine entities with circuit breaker support.
 */
@Repository
public interface AsnLineRepository extends BaseRepository<AsnLine> {
    
    /**
     * Find ASN lines by header ID
     *
     * @param headerId The ID of the header
     * @return List of ASN lines
     */
    List<AsnLine> findByHeader_Id(Long headerId);
    
    /**
     * Find ASN lines by header ID with pagination
     *
     * @param headerId The ID of the header
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByHeader_Id(Long headerId, Pageable pageable);
    
    /**
     * Find ASN lines by client ID and header ID with pagination
     *
     * @param clientId The ID of the client
     * @param headerId The ID of the header
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId, Pageable pageable);
    
    /**
     * Find ASN lines by client ID and item number with pagination
     *
     * @param clientId The ID of the client
     * @param itemNumber The item number
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable);
    
    /**
     * Find ASN lines by client ID and lot number with pagination
     *
     * @param clientId The ID of the client
     * @param lotNumber The lot number
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndLotNumber(Long clientId, String lotNumber, Pageable pageable);
    
    /**
     * Find ASN lines by client ID and status with pagination
     *
     * @param clientId The ID of the client
     * @param status The status
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);
    
    /**
     * Find ASN lines by client ID and quantity greater than with pagination
     *
     * @param clientId The ID of the client
     * @param quantity The quantity threshold
     * @param pageable Pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable);

    /**
     * Find ASN lines by client ID and header ID
     *
     * @param clientId The ID of the client
     * @param headerId The ID of the header
     * @return List of ASN lines
     */
    List<AsnLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId);

    /**
     * Find ASN lines by client ID and item number
     *
     * @param clientId The ID of the client
     * @param itemNumber The item number
     * @return List of ASN lines
     */
    List<AsnLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber);

    /**
     * Find ASN lines by client ID and lot number
     *
     * @param clientId The ID of the client
     * @param lotNumber The lot number
     * @return List of ASN lines
     */
    List<AsnLine> findByClient_IdAndLotNumber(Long clientId, String lotNumber);

    /**
     * Find ASN lines by client ID and status
     *
     * @param clientId The ID of the client
     * @param status The status
     * @return List of ASN lines
     */
    List<AsnLine> findByClient_IdAndStatus(Long clientId, String status);

    /**
     * Find ASN lines by client ID and quantity greater than
     *
     * @param clientId The ID of the client
     * @param quantity The quantity threshold
     * @return List of ASN lines
     */
    List<AsnLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity);

    /**
     * Delete ASN lines by header IDs
     *
     * @param headerIds List of header IDs
     */
    void deleteByHeaderIdIn(List<Long> headerIds);
} 
