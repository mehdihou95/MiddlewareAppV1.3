package com.middleware.shared.repository.connectors;

import com.middleware.shared.model.connectors.As2Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface As2ConfigRepository extends JpaRepository<As2Config, Long> {
    List<As2Config> findByActiveTrue();
    Optional<As2Config> findByClient_IdAndInterfaceConfig_Id(Long clientId, Long interfaceId);
    List<As2Config> findByClient_Id(Long clientId);
    List<As2Config> findByInterfaceConfig_Id(Long interfaceId);
} 