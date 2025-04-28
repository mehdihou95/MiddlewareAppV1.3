package com.middleware.processor.service.interfaces;
    
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
    
/**
 * Service for managing ASN (Advanced Shipping Notice) documents.
 */
public interface AsnService {
    /**
     * Create a new ASN header.
     *
     * @param header The ASN header to create
     * @return The created ASN header
     */
    AsnHeader createAsnHeader(AsnHeader header);

    /**
     * Get an ASN header by its ID.
     *
     * @param id The ID of the ASN header
     * @return Optional containing the ASN header if found
     */
    Optional<AsnHeader> getAsnHeaderById(Long id);

    /**
     * Get all ASN headers with pagination.
     *
     * @param pageable The pagination information
     * @return Page of ASN headers
     */
    Page<AsnHeader> getAllAsnHeaders(Pageable pageable);

    /**
     * Get all ASN headers.
     *
     * @return List of all ASN headers
     */
    List<AsnHeader> getAllAsnHeaders();

    /**
     * Get ASN headers for a specific client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of ASN headers
     */
    Page<AsnHeader> getAsnHeadersByClient(Long clientId, Pageable pageable);

    /**
     * Get ASN headers for a specific client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of ASN headers
     */
    Page<AsnHeader> getAsnHeadersByClient_Id(Long clientId, Pageable pageable);

    /**
     * Get ASN headers for a specific client.
     *
     * @param clientId The ID of the client
     * @return List of ASN headers for the client
     */
    List<AsnHeader> getAsnHeadersByClient(Long clientId);

    /**
     * Find an ASN header by document number and client ID.
     *
     * @param asnNumber The document number
     * @param clientId The ID of the client
     * @return Optional containing the ASN header if found
     */
    Optional<AsnHeader> findByAsnNumberAndClient_Id(String asnNumber, Long clientId);

    /**
     * Find ASN headers by client ID and shipment date range with pagination.
     *
     * @param clientId The ID of the client
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable The pagination information
     * @return Page of ASN headers
     */
    Page<AsnHeader> findByClient_IdAndReceiptDttmBetween(Long clientId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Update an ASN header.
     *
     * @param id The ID of the ASN header to update
     * @param header The updated ASN header
     * @return The updated ASN header
     */
    AsnHeader updateAsnHeader(Long id, AsnHeader headerDetails);

    /**
     * Delete an ASN header.
     *
     * @param id The ID of the ASN header to delete
     */
    void deleteAsnHeader(Long id);
    
    /**
     * Create a new ASN line.
     *
     * @param line The ASN line to create
     * @return The created ASN line
     */
    AsnLine createAsnLine(AsnLine line);

    /**
     * Get an ASN line by its ID.
     *
     * @param id The ID of the ASN line
     * @return Optional containing the ASN line if found
     */
    Optional<AsnLine> getAsnLineById(Long id);

    /**
     * Get all ASN lines with pagination.
     *
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> getAllAsnLines(Pageable pageable);

    /**
     * Get ASN lines by header ID with pagination.
     *
     * @param headerId The ID of the ASN header
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> getAsnLinesByHeader(Long headerId, Pageable pageable);

    /**
     * Get ASN lines by header ID.
     *
     * @param headerId The ID of the ASN header
     * @return List of ASN lines
     */
    List<AsnLine> getAsnLinesByHeader(Long headerId);

    /**
     * Get ASN lines by client ID with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> getAsnLinesByClient_Id(Long clientId, Pageable pageable);

    /**
     * Find ASN lines by client ID and header ID with pagination.
     *
     * @param clientId The ID of the client
     * @param headerId The ID of the ASN header
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId, Pageable pageable);

    /**
     * Find ASN lines by client ID and item number with pagination.
     *
     * @param clientId The ID of the client
     * @param itemNumber The item number
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable);

    /**
     * Find ASN lines by client ID and lot number with pagination.
     *
     * @param clientId The ID of the client
     * @param lotNumber The lot number
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndLotNumber(Long clientId, String lotNumber, Pageable pageable);

    /**
     * Find ASN lines by client ID and status with pagination.
     *
     * @param clientId The ID of the client
     * @param status The status
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find ASN lines by client ID and quantity greater than with pagination.
     *
     * @param clientId The ID of the client
     * @param quantity The quantity threshold
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable);

    /**
     * Update an ASN line.
     *
     * @param id The ID of the ASN line to update
     * @param line The updated ASN line
     * @return The updated ASN line
     */
    AsnLine updateAsnLine(Long id, AsnLine lineDetails);

    /**
     * Delete an ASN line.
     *
     * @param id The ID of the ASN line to delete
     */
    void deleteAsnLine(Long id);

    /**
     * Create multiple ASN lines.
     *
     * @param lines The list of ASN lines to create
     * @return The list of created ASN lines
     */
    List<AsnLine> createAsnLines(List<AsnLine> lines);

    /**
     * Find ASN header by number.
     *
     * @param asnNumber The ASN number
     * @return Optional containing the ASN header if found
     */
    Optional<AsnHeader> findByAsnNumber(String asnNumber);

    /**
     * Get ASN lines by header ID with pagination.
     *
     * @param headerId The ID of the ASN header
     * @param pageable The pagination information
     * @return Page of ASN lines
     */
    Page<AsnLine> getAsnLinesByHeader_Id(Long headerId, Pageable pageable);
} 
