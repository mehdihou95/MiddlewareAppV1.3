package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.MappingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

/**
 * Service for managing MappingRule entities.
 * Provides CRUD operations and search functionality for mapping rules.
 */
public interface MappingRuleService {
    /**
     * Get all mapping rules with pagination.
     *
     * @param pageable The pagination information
     * @return Page of mapping rules
     */
    Page<MappingRule> getAllMappingRules(Pageable pageable);

    /**
     * Get mapping rules by client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of mapping rules for the client
     */
    Page<MappingRule> getMappingRulesByClient(Long clientId, Pageable pageable);

    /**
     * Get mapping rules by interface with pagination.
     *
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of mapping rules for the interface
     */
    Page<MappingRule> getMappingRulesByInterface(Long interfaceId, Pageable pageable);

    /**
     * Get mapping rules by client and interface with pagination.
     *
     * @param clientId The ID of the client
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of mapping rules for the client and interface
     */
    Page<MappingRule> getMappingRulesByClientAndInterface(Long clientId, Long interfaceId, Pageable pageable);

    /**
     * Get active mapping rules for an interface with pagination.
     *
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of active mapping rules for the interface
     */
    Page<MappingRule> getActiveMappingRules(Long interfaceId, Pageable pageable);

    /**
     * Create a new mapping rule.
     *
     * @param mappingRule The mapping rule to create
     * @return The created mapping rule
     */
    MappingRule createMappingRule(MappingRule mappingRule);

    /**
     * Update an existing mapping rule.
     *
     * @param id The ID of the mapping rule to update
     * @param mappingRule The updated mapping rule
     * @return The updated mapping rule
     */
    MappingRule updateMappingRule(Long id, MappingRule mappingRule);

    /**
     * Delete a mapping rule.
     *
     * @param id The ID of the mapping rule to delete
     */
    void deleteMappingRule(Long id);

    /**
     * Get mapping rules with filters and pagination.
     *
     * @param pageable The pagination information
     * @param nameFilter The name filter
     * @param isActiveFilter The active status filter
     * @return Page of filtered mapping rules
     */
    Page<MappingRule> getMappingRules(Pageable pageable, String nameFilter, Boolean isActiveFilter);

    /**
     * Get a mapping rule by ID.
     *
     * @param id The ID of the mapping rule
     * @return Optional containing the mapping rule if found
     */
    Optional<MappingRule> getMappingRuleById(Long id);

    /**
     * Search mapping rules by name with pagination.
     *
     * @param name The name to search for
     * @param pageable The pagination information
     * @return Page of matching mapping rules
     */
    Page<MappingRule> searchMappingRules(String name, Pageable pageable);

    /**
     * Get mapping rules by status with pagination.
     *
     * @param isActive The active status to filter by
     * @param pageable The pagination information
     * @return Page of mapping rules with the specified status
     */
    Page<MappingRule> getMappingRulesByStatus(boolean isActive, Pageable pageable);

    /**
     * Get mapping rules by client ID with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of mapping rules for the client
     */
    Page<MappingRule> getMappingRulesByClientId(Long clientId, Pageable pageable);

    /**
     * Find mapping rules by table name and client ID with pagination.
     *
     * @param tableName The name of the table
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of matching mapping rules
     */
    Page<MappingRule> findByTableNameAndClient_Id(String tableName, Long clientId, Pageable pageable);

    /**
     * Delete mapping rules by client ID and table name.
     *
     * @param clientId The ID of the client
     * @param tableName The name of the table
     */
    void deleteByClient_IdAndTableName(Long clientId, String tableName);

    /**
     * Save mapping configuration.
     *
     * @param rules The list of mapping rules to save
     */
    void saveMappingConfiguration(List<MappingRule> rules);

    /**
     * Get mapping rules by client ID, interface ID, and table name with pagination.
     *
     * @param clientId The ID of the client
     * @param interfaceId The ID of the interface
     * @param tableName The name of the table
     * @param pageable The pagination information
     * @return Page of mapping rules for the client, interface, and table
     */
    Page<MappingRule> findByClientIdAndInterfaceIdAndTableName(
        Long clientId, 
        Long interfaceId, 
        String tableName, 
        Pageable pageable
    );

    /**
     * Updates the target level for all mapping rules based on their table name.
     */
    void updateTargetLevels();
} 
