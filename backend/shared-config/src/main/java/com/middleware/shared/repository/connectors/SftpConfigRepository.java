package com.middleware.shared.repository.connectors;

import com.middleware.shared.model.connectors.SftpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SftpConfigRepository extends JpaRepository<SftpConfig, Long> {
    List<SftpConfig> findByActiveTrue();
    
    @Query("SELECT sc FROM SftpConfig sc JOIN FETCH sc.client JOIN FETCH sc.interfaceEntity WHERE sc.active = true")
    List<SftpConfig> findByActiveTrueWithRelationships();
    
    Optional<SftpConfig> findByClient_IdAndInterfaceEntity_Id(Long clientId, Long interfaceId);
    List<SftpConfig> findByClient_Id(Long clientId);
    List<SftpConfig> findByInterfaceEntity_Id(Long interfaceId);
} 
