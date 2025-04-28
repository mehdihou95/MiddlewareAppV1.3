package com.middleware.shared.repository;

import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MappingRule entities with circuit breaker support.
 */
@Repository
public interface MappingRuleRepository extends BaseRepository<MappingRule> {
    
    /**
     * Find mapping rule by client ID and name
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId AND m.name = :name")
    Optional<MappingRule> findByClient_IdAndName(
        @Param("clientId") Long clientId,
        @Param("name") String name);
    
    /**
     * Find mapping rules by client ID and source field
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId AND m.sourceField = :sourceField")
    List<MappingRule> findByClient_IdAndSourceField(
        @Param("clientId") Long clientId,
        @Param("sourceField") String sourceField);
    
    /**
     * Find mapping rules by client ID and target field
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId AND m.targetField = :targetField")
    List<MappingRule> findByClient_IdAndTargetField(
        @Param("clientId") Long clientId,
        @Param("targetField") String targetField);
    
    /**
     * Find mapping rules by client ID ordered by priority
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId ORDER BY m.priority ASC")
    List<MappingRule> findByClient_IdOrderByPriority(@Param("clientId") Long clientId);
    
    /**
     * Find required mapping rules by client ID
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId AND m.required = true")
    List<MappingRule> findRequiredRulesByClient_Id(@Param("clientId") Long clientId);

    /**
     * Find mapping rules by interface ID
     */
    List<MappingRule> findByInterfaceId(Long interfaceId);

    /**
     * Find mapping rules by interface ID with pagination
     */
    Page<MappingRule> findByInterfaceId(Long interfaceId, Pageable pageable);

    /**
     * Find mapping rules by name containing with pagination
     */
    Page<MappingRule> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find mapping rules by active status with pagination
     */
    Page<MappingRule> findByIsActive(boolean isActive, Pageable pageable);

    /**
     * Find active mapping rules by interface ID
     */
    List<MappingRule> findByInterfaceIdAndIsActiveTrue(Long interfaceId);

    /**
     * Find mapping rules by interface ID and active status
     */
    List<MappingRule> findByInterfaceIdAndIsActive(Long interfaceId, boolean isActive);

    /**
     * Check if mapping rule exists by name and interface ID
     */
    boolean existsByNameAndInterfaceId(String name, Long interfaceId);

    /**
     * Check if mapping rule exists by name and interface ID excluding an ID
     */
    boolean existsByNameAndInterfaceIdAndIdNot(String name, Long interfaceId, Long id);

    /**
     * Find mapping rules by table name and client ID
     */
    @Query("SELECT m FROM MappingRule m WHERE m.tableName = :tableName AND m.client.id = :clientId")
    List<MappingRule> findByTableNameAndClient_Id(
        @Param("tableName") String tableName,
        @Param("clientId") Long clientId);

    /**
     * Delete mapping rules by client ID and table name
     */
    @Modifying
    @Query("DELETE FROM MappingRule m WHERE m.client.id = :clientId AND m.tableName = :tableName")
    void deleteByClient_IdAndTableName(
        @Param("clientId") Long clientId,
        @Param("tableName") String tableName);

    /**
     * Find default mapping rules
     */
    List<MappingRule> findByIsDefaultTrue();

    /**
     * Find mapping rules by client
     */
    List<MappingRule> findByClient(Client client);

    /**
     * Find mapping rules by table name
     */
    List<MappingRule> findByTableName(String tableName);

    /**
     * Find active mapping rules by client ID with pagination
     */
    @Query("SELECT m FROM MappingRule m WHERE m.client.id = :clientId AND m.isActive = true")
    Page<MappingRule> findByClient_IdAndIsActiveTrue(
        @Param("clientId") Long clientId,
        Pageable pageable);

    /**
     * Find mapping rules by name containing and active status with pagination
     */
    @Query("SELECT m FROM MappingRule m WHERE m.name LIKE %:name% AND m.isActive = :isActive")
    Page<MappingRule> findByNameContainingIgnoreCaseAndIsActive(
        @Param("name") String name,
        @Param("isActive") boolean isActive,
        Pageable pageable);

    /**
     * Delete mapping rules by interface ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MappingRule m WHERE m.interfaceEntity.id = :interfaceId")
    void deleteByInterfaceId(@Param("interfaceId") Long interfaceId);

    /**
     * Find mapping rules by interface ID with pagination
     */
    Page<MappingRule> findByInterfaceEntity_Id(Long interfaceId, Pageable pageable);

    /**
     * Find mapping rules by interface ID and table name with pagination
     */
    Page<MappingRule> findByInterfaceEntity_IdAndTableName(
        Long interfaceId,
        String tableName,
        Pageable pageable);

    /**
     * Find mapping rules by interface ID and table name
     */
    List<MappingRule> findByInterfaceEntity_IdAndTableName(Long interfaceId, String tableName);

    /**
     * Find mapping rules by interface ID and active status
     */
    List<MappingRule> findByInterfaceEntity_IdAndIsActive(Long interfaceId, Boolean isActive);

    /**
     * Find mapping rules by interface ID, table name, and active status
     */
    List<MappingRule> findByInterfaceEntity_IdAndTableNameAndIsActive(
        Long interfaceId,
        String tableName,
        Boolean isActive);

    /**
     * Find mapping rules by client ID, table name, and active status
     */
    List<MappingRule> findByClient_IdAndTableNameAndIsActive(
        Long clientId,
        String tableName,
        Boolean isActive);

    /**
     * Find mapping rules by client ID and interface ID
     */
    List<MappingRule> findByClient_IdAndInterfaceEntity_Id(Long clientId, Long interfaceId);
} 
