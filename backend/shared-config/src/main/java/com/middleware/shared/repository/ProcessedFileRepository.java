package com.middleware.shared.repository;

import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessedFile entities with circuit breaker support.
 */
@Repository
public interface ProcessedFileRepository extends BaseRepository<ProcessedFile> {

    /**
     * Find processed files by client ID and interface ID with pagination
     *
     * @param clientId The ID of the client
     * @param interfaceId The ID of the interface
     * @param pageable Pagination information
     * @return Page of processed files
     */
    @Query("SELECT p FROM ProcessedFile p WHERE p.client.id = :clientId AND p.interfaceEntity.id = :interfaceId")
    Page<ProcessedFile> findByClient_IdAndInterfaceEntity_Id(
        @Param("clientId") Long clientId,
        @Param("interfaceId") Long interfaceId,
        Pageable pageable);

    /**
     * Find processed files by client ID and status
     *
     * @param clientId The ID of the client
     * @param status The status to filter by
     * @return List of processed files
     */
    @Query("SELECT p FROM ProcessedFile p WHERE p.client.id = :clientId AND p.status = :status")
    List<ProcessedFile> findByClient_IdAndStatus(@Param("clientId") Long clientId, @Param("status") String status);

    /**
     * Find processed files by client ID and filename
     *
     * @param clientId The ID of the client
     * @param fileName The filename to filter by
     * @return List of processed files
     */
    List<ProcessedFile> findByClient_IdAndFileName(Long clientId, String fileName);

    /**
     * Find processed files by status
     *
     * @param status The status to filter by
     * @return List of processed files
     */
    List<ProcessedFile> findByStatus(String status);

    /**
     * Find processed files by filename containing (case-insensitive) with pagination
     *
     * @param fileName The filename to search for
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);

    /**
     * Find processed files by status with pagination
     *
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByStatus(String status, Pageable pageable);

    /**
     * Find processed files by date range with pagination
     *
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByProcessedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find processed files by client ID and status with pagination
     *
     * @param clientId The ID of the client
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByClientIdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find processed files by client ID and date range with pagination
     *
     * @param clientId The ID of the client
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByClientIdAndProcessedAtBetween(
        Long clientId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable);

    /**
     * Find processed files by creation date range with pagination
     *
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find processed files by client ID and error message not null
     *
     * @param clientId The ID of the client
     * @return List of processed files with errors
     */
    List<ProcessedFile> findByClient_IdAndErrorMessageIsNotNull(Long clientId);

    /**
     * Find top 10 most recent processed files by client ID
     *
     * @param clientId The ID of the client
     * @return List of processed files
     */
    List<ProcessedFile> findTop10ByClient_IdOrderByProcessedAtDesc(Long clientId);

    /**
     * Find processed files by client
     *
     * @param client The client entity
     * @return List of processed files
     */
    List<ProcessedFile> findByClient(Client client);

    /**
     * Find processed files by interface
     *
     * @param interfaceEntity The interface entity
     * @return List of processed files
     */
    List<ProcessedFile> findByInterfaceEntity(Interface interfaceEntity);

    /**
     * Find processed files by interface ID with pagination
     *
     * @param interfaceId The ID of the interface
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByInterfaceEntity_Id(Long interfaceId, Pageable pageable);

    /**
     * Find processed files by status and date range with pagination
     *
     * @param status The status to filter by
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param pageable Pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> findByStatusAndProcessedAtBetween(
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable);

    /**
     * Find processed files by search criteria with pagination
     *
     * @param searchTerm The search term for filename
     * @param status The status to filter by
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param pageable Pagination information
     * @return Page of processed files
     */
    @Query("SELECT p FROM ProcessedFile p WHERE " +
           "(:searchTerm IS NULL OR p.fileName LIKE %:searchTerm%) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:startDate IS NULL OR p.processedAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.processedAt <= :endDate)")
    Page<ProcessedFile> findBySearchCriteria(
        @Param("searchTerm") String searchTerm,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Delete processed files by interface ID
     *
     * @param interfaceId The ID of the interface
     */
    @Modifying
    @Query("DELETE FROM ProcessedFile p WHERE p.interfaceEntity.id = :interfaceId")
    void deleteByInterfaceId(@Param("interfaceId") Long interfaceId);

    /**
     * Find a processed file by its filename and interface ID
     *
     * @param fileName The name of the file
     * @param interfaceId The ID of the interface
     * @return Optional containing the processed file if found
     */
    Optional<ProcessedFile> findByFileNameAndInterfaceEntity_Id(String fileName, Long interfaceId);

    /**
     * Find the most recent processed files by filename and interface ID
     *
     * @param fileName The name of the file
     * @param interfaceId The ID of the interface
     * @param pageable Pagination information
     * @return List of processed files ordered by creation date
     */
    @Query("SELECT p FROM ProcessedFile p WHERE p.fileName = :fileName AND p.interfaceEntity.id = :interfaceId ORDER BY p.createdAt DESC")
    List<ProcessedFile> findMostRecentByFileNameAndInterfaceId(
        @Param("fileName") String fileName,
        @Param("interfaceId") Long interfaceId,
        Pageable pageable);

    /**
     * Find processed files by client ID with pagination
     *
     * @param clientId The ID of the client
     * @param pageable Pagination information
     * @return Page of processed files
     */
    @Query("SELECT p FROM ProcessedFile p WHERE p.client.id = :clientId")
    Page<ProcessedFile> findByClient_Id(@Param("clientId") Long clientId, Pageable pageable);
} 
