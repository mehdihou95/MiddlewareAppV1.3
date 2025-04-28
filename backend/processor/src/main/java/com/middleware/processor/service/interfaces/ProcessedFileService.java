package com.middleware.processor.service.interfaces;
    
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
    
/**
 * Service interface for processed file operations.
 */
public interface ProcessedFileService {
    /**
     * Get all processed files with pagination.
     *
     * @param pageable The pagination information
     * @return Page of processed files
     */
    Page<ProcessedFile> getAllProcessedFiles(Pageable pageable);

    /**
     * Get processed files by client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of processed files for the client
     */
    Page<ProcessedFile> getProcessedFilesByClient(Long clientId, Pageable pageable);

    /**
     * Get processed files by interface with pagination.
     *
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of processed files for the interface
     */
    Page<ProcessedFile> getProcessedFilesByInterface(Long interfaceId, Pageable pageable);

    /**
     * Get processed files by date range with pagination.
     *
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable The pagination information
     * @return Page of processed files within the date range
     */
    Page<ProcessedFile> getProcessedFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get processed files by status with pagination.
     *
     * @param status The status to filter by
     * @param pageable The pagination information
     * @return Page of processed files with the specified status
     */
    Page<ProcessedFile> getProcessedFilesByStatus(String status, Pageable pageable);

    /**
     * Get a processed file by ID.
     *
     * @param id The ID of the processed file
     * @return Optional containing the processed file if found
     */
    Optional<ProcessedFile> getProcessedFileById(Long id);

    /**
     * Create a new processed file.
     *
     * @param processedFile The processed file to create
     * @return The created processed file
     */
    ProcessedFile createProcessedFile(ProcessedFile processedFile);

    /**
     * Update an existing processed file.
     *
     * @param id The ID of the processed file to update
     * @param processedFile The updated processed file data
     * @return The updated processed file
     */
    ProcessedFile updateProcessedFile(Long id, ProcessedFile processedFile);

    /**
     * Delete a processed file.
     *
     * @param id The ID of the processed file to delete
     */
    void deleteProcessedFile(Long id);

    /**
     * Get all error files with pagination.
     *
     * @param pageable The pagination information
     * @return Page of error files
     */
    Page<ProcessedFile> getErrorFiles(Pageable pageable);

    /**
     * Get error files by date range with pagination.
     *
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable The pagination information
     * @return Page of error files within the date range
     */
    Page<ProcessedFile> getErrorFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get processed files with advanced filtering and pagination.
     *
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param direction The sort direction
     * @param fileNameFilter The search term
     * @param statusFilter The status to filter by
     * @param startDate The start date
     * @param endDate The end date
     * @param clientId The client ID to filter by
     * @param interfaceId The interface ID to filter by
     * @return Page of processed files matching the criteria
     */
    Page<ProcessedFile> getProcessedFiles(int page, int size, String sortBy, String direction,
                                        String fileNameFilter, String statusFilter,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        Long clientId, Long interfaceId);

    /**
     * Get processed files by client with pagination and sorting.
     *
     * @param clientId The ID of the client
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of processed files for the client
     */
    Page<ProcessedFile> getProcessedFilesByClient(Long clientId, int page, int size, String sortBy, String sortDirection);

    /**
     * Get processed files by client and status with pagination and sorting.
     *
     * @param clientId The ID of the client
     * @param status The status to filter by
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of processed files for the client with the specified status
     */
    Page<ProcessedFile> getProcessedFilesByClientAndStatus(Long clientId, String status, 
                                                         int page, int size, String sortBy, String sortDirection);

    /**
     * Get processed files by client and date range with pagination and sorting.
     *
     * @param clientId The ID of the client
     * @param startDate The start date
     * @param endDate The end date
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of processed files for the client within the date range
     */
    Page<ProcessedFile> getProcessedFilesByClientAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate,
                                                            int page, int size, String sortBy, String sortDirection);

    /**
     * Get processed files by status.
     *
     * @param status The status to filter by
     * @return List of processed files with the specified status
     */
    List<ProcessedFile> getProcessedFilesByStatus(String status);

    Page<ProcessedFile> getProcessedFilesByClient(Long clientId, PageRequest pageRequest);

    Page<ProcessedFile> searchProcessedFiles(String fileName, PageRequest pageRequest);

    Page<ProcessedFile> getProcessedFilesByStatus(String status, PageRequest pageRequest);

    Page<ProcessedFile> getProcessedFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, PageRequest pageRequest);

    Page<ProcessedFile> getProcessedFilesByClientAndStatus(Long clientId, String status, PageRequest pageRequest);

    Page<ProcessedFile> getProcessedFilesByClientAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate, PageRequest pageRequest);

    /**
     * Find a processed file by filename and interface ID.
     *
     * @param fileName The name of the file
     * @param interfaceId The ID of the interface
     * @return Optional containing the processed file if found
     */
    Optional<ProcessedFile> findByFileNameAndInterfaceId(String fileName, Long interfaceId);

    /**
     * Find the most recent processed file by filename and interface ID.
     *
     * @param fileName The name of the file
     * @param interfaceId The ID of the interface
     * @return Optional containing the most recent processed file if found
     */
    Optional<ProcessedFile> findMostRecentByFileNameAndInterfaceId(String fileName, Long interfaceId);

    /**
     * Find processed files with various filters and pagination.
     *
     * @param clientId The client ID to filter by
     * @param interfaceId The interface ID to filter by
     * @param status The status to filter by
     * @param fileName The filename to search for
     * @param startDate The start date for date range filtering
     * @param endDate The end date for date range filtering
     * @param pageable The pagination information
     * @return Page of processed files matching the criteria
     */
    Page<ProcessedFile> findProcessedFiles(
            Long clientId, 
            Long interfaceId, 
            String status, 
            String fileName, 
            LocalDateTime startDate, 
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find an existing processed file or create a new one if it doesn't exist.
     * This method ensures atomic find-or-create operation with proper transaction handling.
     *
     * @param fileName The name of the file
     * @param interfaceEntity The interface entity
     * @param defaultFile The default file to create if none exists
     * @return The existing or newly created processed file
     */
    ProcessedFile findOrCreateProcessedFile(String fileName, Interface interfaceEntity, ProcessedFile defaultFile);

    /**
     * Atomically update a processed file's status and content.
     * This method ensures that updates are performed on the most recent version of the record.
     *
     * @param id The ID of the processed file
     * @param status The new status
     * @param errorMessage The error message (if any)
     * @param content The file content (if any)
     * @return The updated processed file
     */
    ProcessedFile atomicUpdateProcessedFile(Long id, String status, String errorMessage, String content);
}