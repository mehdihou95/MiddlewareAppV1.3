package com.middleware.shared.repository;

import com.middleware.shared.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Client entities with circuit breaker support.
 */
@Repository
public interface ClientRepository extends BaseRepository<Client> {
    
    /**
     * Find client by name
     *
     * @param name The name of the client
     * @return Optional containing the client if found
     */
    Optional<Client> findByName(String name);

    /**
     * Find client by code
     *
     * @param code The code of the client
     * @return Optional containing the client if found
     */
    Optional<Client> findByCode(String code);

    /**
     * Check if client exists by name
     *
     * @param name The name to check
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Check if client exists by name excluding a specific ID
     *
     * @param name The name to check
     * @param id The ID to exclude
     * @return true if exists, false otherwise
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Find clients by name containing (case-insensitive) with pagination
     *
     * @param name The name to search for
     * @param pageable Pagination information
     * @return Page of clients
     */
    Page<Client> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find clients by status with pagination
     *
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of clients
     */
    Page<Client> findByStatus(String status, Pageable pageable);

    /**
     * Find clients by name containing and status with pagination
     *
     * @param name The name to search for
     * @param status The status to filter by
     * @param pageable Pagination information
     * @return Page of clients
     */
    Page<Client> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    /**
     * Check if client exists by code
     *
     * @param code The code to check
     * @return true if exists, false otherwise
     */
    boolean existsByCode(String code);
} 
