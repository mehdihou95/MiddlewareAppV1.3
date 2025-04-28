package com.middleware.shared.repository;
    
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
    
import java.util.List;
import java.util.Optional;
    
/**
 * Repository interface for Interface entities.
 * Provides database operations for Interface management with circuit breaker support.
 */
@Repository
public interface InterfaceRepository extends BaseRepository<Interface> {
    
    /**
     * Finds interfaces by client.
     *
     * @param client The client to filter by
     * @return List of interfaces for the client
     */
    List<Interface> findByClient(Client client);
    
    /**
     * Finds interfaces by client ID.
     *
     * @param clientId The ID of the client
     * @return List of interfaces for the client
     */
    List<Interface> findByClient_Id(Long clientId);
    
    /**
     * Finds an interface by name and client ID.
     *
     * @param name The name of the interface
     * @param clientId The ID of the client
     * @return Optional containing the interface if found
     */
    Optional<Interface> findByNameAndClient_Id(String name, Long clientId);
    
    /**
     * Finds interfaces by client ID with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of interfaces for the client
     */
    Page<Interface> findByClient_Id(Long clientId, Pageable pageable);
    
    /**
     * Finds interfaces by active state.
     *
     * @param isActive The active state to filter by
     * @param pageable The pagination information
     * @return Page of matching interfaces
     */
    Page<Interface> findByIsActive(Boolean isActive, Pageable pageable);
    
    /**
     * Finds interfaces by name containing.
     *
     * @param name The name to search for
     * @param pageable The pagination information
     * @return Page of matching interfaces
     */
    Page<Interface> findByNameContaining(String name, Pageable pageable);
    
    /**
     * Finds interfaces by type.
     *
     * @param type The type to filter by
     * @param pageable The pagination information
     * @return Page of matching interfaces
     */
    Page<Interface> findByType(String type, Pageable pageable);
    
    /**
     * Checks if an interface exists by name and client ID.
     *
     * @param name The name to check
     * @param clientId The ID of the client
     * @return true if the interface exists, false otherwise
     */
    boolean existsByNameAndClient_Id(String name, Long clientId);

    /**
     * Checks if an interface exists by name and client ID, excluding a specific ID.
     *
     * @param name The name to check
     * @param clientId The ID of the client
     * @param id The ID to exclude
     * @return true if the interface exists, false otherwise
     */
    boolean existsByNameAndClient_IdAndIdNot(String name, Long clientId, Long id);

    /**
     * Finds an interface by ID and client ID.
     *
     * @param id The ID of the interface
     * @param clientId The ID of the client
     * @return Optional containing the interface if found
     */
    Optional<Interface> findByIdAndClient_Id(Long id, Long clientId);

    /**
     * Finds interfaces by client ID and type.
     *
     * @param clientId The ID of the client
     * @param type The type to filter by
     * @return List of matching interfaces
     */
    List<Interface> findByClient_IdAndType(Long clientId, String type);

    /**
     * Finds active interfaces by client ID.
     *
     * @param clientId The ID of the client
     * @return List of active interfaces for the client
     */
    List<Interface> findByClient_IdAndIsActiveTrue(Long clientId);

    /**
     * Find an interface by its root element name and active status.
     *
     * @param rootElement The root element name
     * @return Optional containing the interface if found
     */
    Optional<Interface> findByRootElementAndIsActiveTrue(String rootElement);

    /**
     * Find interfaces by type and active status.
     *
     * @param type The interface type
     * @param isActive The active status
     * @return List of matching interfaces
     */
    List<Interface> findByTypeAndIsActive(String type, boolean isActive);

    /**
     * Finds interfaces by client ID and active status.
     *
     * @param clientId The ID of the client
     * @param isActive The active status
     * @return List of matching interfaces
     */
    List<Interface> findByClient_IdAndIsActive(Long clientId, boolean isActive);
} 
