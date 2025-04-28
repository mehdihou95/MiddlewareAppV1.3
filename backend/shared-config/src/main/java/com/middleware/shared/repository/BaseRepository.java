package com.middleware.shared.repository;

import com.middleware.shared.model.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface with circuit breaker support.
 * Provides common methods for all repositories.
 *
 * @param <T> Entity type
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    /**
     * Find all entities by client ID.
     *
     * @param clientId Client ID
     * @return List of entities
     */
    List<T> findByClient_Id(Long clientId);

    /**
     * Find all entities by client ID with pagination.
     *
     * @param clientId Client ID
     * @param pageable Pagination information
     * @return Page of entities
     */
    Page<T> findByClient_Id(Long clientId, Pageable pageable);

    /**
     * Find entity by ID and client ID.
     *
     * @param id Entity ID
     * @param clientId Client ID
     * @return Optional entity
     */
    Optional<T> findByIdAndClient_Id(Long id, Long clientId);

    /**
     * Delete all entities by client ID.
     *
     * @param clientId Client ID
     */
    void deleteByClient_Id(Long clientId);
}
