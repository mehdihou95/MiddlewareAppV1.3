package com.middleware.processor.service.interfaces;
    
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
    
/**
 * Service for managing Interface entities.
 * Provides CRUD operations, search functionality, and client-specific operations.
 */
public interface InterfaceService {
    /**
     * Retrieves all interfaces.
     * 
     * @return List of all interfaces
     */
    List<Interface> getAllInterfaces();

    /**
     * Retrieves an interface by its ID.
     *
     * @param id The ID of the interface to retrieve
     * @return Optional containing the interface if found
     */
    Optional<Interface> getInterfaceById(Long id);

    /**
     * Creates a new interface.
     *
     * @param interfaceEntity The interface to create
     * @return The created interface
     * @throws ValidationException if the interface is invalid or a duplicate
     */
    @CacheEvict(value = "interfaces", allEntries = true)
    Interface createInterface(Interface interfaceEntity);

    /**
     * Updates an existing interface.
     *
     * @param id The ID of the interface to update
     * @param interfaceEntity The updated interface data
     * @return The updated interface
     * @throws ResourceNotFoundException if the interface is not found
     * @throws ValidationException if the update data is invalid
     */
    @CacheEvict(value = "interfaces", key = "#id")
    Interface updateInterface(Long id, Interface interfaceEntity);

    /**
     * Deletes an interface.
     *
     * @param id The ID of the interface to delete
     * @throws ResourceNotFoundException if the interface is not found
     */
    @CacheEvict(value = "interfaces", key = "#id")
    void deleteInterface(Long id);
    
    /**
     * Retrieves interfaces by client.
     *
     * @param client The client to filter by
     * @return List of interfaces for the client
     */
    List<Interface> getInterfacesByClient(Client client);

    /**
     * Retrieves interfaces by client ID.
     *
     * @param clientId The ID of the client
     * @return List of interfaces for the client
     */
    List<Interface> getClientInterfaces(Long clientId);

    /**
     * Retrieves an interface by name and client ID.
     *
     * @param name The name of the interface
     * @param clientId The ID of the client
     * @return Optional containing the interface if found
     */
    Optional<Interface> getInterfaceByName(String name, Long clientId);
    
    /**
     * Retrieves all interfaces with pagination.
     *
     * @param pageable The pagination information
     * @return Page of interfaces
     */
    @Cacheable(value = "interfaces", key = "'all'")
    Page<Interface> getAllInterfaces(Pageable pageable);

    /**
     * Retrieves interfaces by client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of interfaces for the client
     */
    @Cacheable(value = "interfaces", key = "'client_' + #clientId")
    Page<Interface> getInterfacesByClient(Long clientId, Pageable pageable);

    /**
     * Retrieves interfaces by client with pagination and sorting.
     *
     * @param clientId The ID of the client
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of interfaces for the client
     */
    Page<Interface> getInterfacesByClient(Long clientId, int page, int size, String sortBy, String sortDirection);
    
    /**
     * Retrieves interfaces with advanced filtering and pagination.
     *
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @param searchTerm The search term
     * @param isActive The active status filter
     * @return Page of filtered interfaces
     */
    Page<Interface> getInterfaces(int page, int size, String sortBy, String sortDirection, String searchTerm, Boolean isActive);

    /**
     * Searches interfaces by name with pagination.
     *
     * @param name The name to search for
     * @param pageable The pagination information
     * @return Page of matching interfaces
     */
    @Cacheable(value = "interfaces", key = "'search_' + #name")
    Page<Interface> searchInterfaces(String name, Pageable pageable);

    /**
     * Searches interfaces by name with pagination and sorting.
     *
     * @param searchTerm The search term
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of matching interfaces
     */
    Page<Interface> searchInterfaces(String searchTerm, int page, int size, String sortBy, String sortDirection);

    /**
     * Retrieves interfaces by type with pagination.
     *
     * @param type The type to filter by
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of interfaces of the specified type
     */
    Page<Interface> getInterfacesByType(String type, int page, int size, String sortBy, String sortDirection);

    /**
     * Retrieves interfaces by status with pagination.
     *
     * @param isActive The active status to filter by
     * @param pageable The pagination information
     * @return Page of interfaces with the specified status
     */
    @Cacheable(value = "interfaces", key = "'active_' + #isActive")
    Page<Interface> getInterfacesByStatus(boolean isActive, Pageable pageable);

    /**
     * Retrieves interfaces by status with pagination and sorting.
     *
     * @param isActive The active status to filter by
     * @param page The page number
     * @param size The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     * @return Page of interfaces with the specified status
     */
    Page<Interface> getInterfacesByStatus(boolean isActive, int page, int size, String sortBy, String sortDirection);
    
    /**
     * Detects interface type from XML content.
     *
     * @param xmlContent The XML content to analyze
     * @param clientId The ID of the client
     * @return The detected interface
     * @throws ValidationException if the XML content is invalid
     */
    Interface detectInterface(String xmlContent, Long clientId);

    /**
     * Checks if an interface exists by name and client ID.
     *
     * @param name The name to check
     * @param clientId The ID of the client
     * @return true if the interface exists, false otherwise
     */
    boolean existsByNameAndClientId(String name, Long clientId);

    /**
     * Retrieves all mapping rules for an interface.
     *
     * @param interfaceId The ID of the interface
     * @return List of mapping rules for the interface
     */
    List<MappingRule> getInterfaceMappings(Long interfaceId);

    /**
     * Updates mapping rules for an interface.
     *
     * @param interfaceId The ID of the interface
     * @param mappings The new mapping rules
     * @return List of updated mapping rules
     */
    List<MappingRule> updateInterfaceMappings(Long interfaceId, List<MappingRule> mappings);
} 
