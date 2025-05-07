package com.middleware.listener.connectors.as2.service;

import com.middleware.shared.model.connectors.As2Config;
import com.middleware.shared.repository.connectors.As2ConfigRepository;
import com.middleware.shared.exception.ResourceNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class As2ConfigService {
    private final As2ConfigRepository as2ConfigRepository;

    @Transactional(readOnly = true)
    public List<As2Config> getAllConfigurations() {
        return as2ConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<As2Config> getActiveConfigurations() {
        return as2ConfigRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public As2Config getConfiguration(Long id) {
        return as2ConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AS2 Configuration not found with id: " + id));
    }

    @Transactional
    public As2Config createConfiguration(As2Config config) {
        log.info("Creating new AS2 configuration for client: {} and interface: {}", 
                config.getClient().getId(), config.getInterfaceConfig().getId());
        validateConfiguration(config);
        return as2ConfigRepository.save(config);
    }

    @Transactional
    public As2Config updateConfiguration(Long id, As2Config config) {
        As2Config existingConfig = getConfiguration(id);
        validateConfiguration(config);
        
        // Update fields
        existingConfig.setServerId(config.getServerId());
        existingConfig.setPartnerId(config.getPartnerId());
        existingConfig.setLocalId(config.getLocalId());
        existingConfig.setApiName(config.getApiName());
        existingConfig.setEncryptionAlgorithm(config.getEncryptionAlgorithm());
        existingConfig.setSignatureAlgorithm(config.getSignatureAlgorithm());
        existingConfig.setCompression(config.isCompression());
        existingConfig.setMdnMode(config.getMdnMode());
        existingConfig.setMdnDigestAlgorithm(config.getMdnDigestAlgorithm());
        existingConfig.setEncryptMessage(config.isEncryptMessage());
        existingConfig.setSignMessage(config.isSignMessage());
        existingConfig.setRequestMdn(config.isRequestMdn());
        existingConfig.setMdnUrl(config.getMdnUrl());
        existingConfig.setActive(config.isActive());

        log.info("Updating AS2 configuration with id: {}", id);
        return as2ConfigRepository.save(existingConfig);
    }

    @Transactional
    public void deleteConfiguration(Long id) {
        log.info("Deleting AS2 configuration with id: {}", id);
        as2ConfigRepository.deleteById(id);
    }

    @Transactional
    public As2Config toggleActive(Long id) {
        As2Config config = getConfiguration(id);
        config.setActive(!config.isActive());
        log.info("Toggling active status for AS2 configuration with id: {} to: {}", id, config.isActive());
        return as2ConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    public List<As2Config> getConfigurationsByClient(Long clientId) {
        return as2ConfigRepository.findByClient_Id(clientId);
    }

    @Transactional(readOnly = true)
    public List<As2Config> getConfigurationsByInterface(Long interfaceId) {
        return as2ConfigRepository.findByInterfaceConfig_Id(interfaceId);
    }

    @Transactional(readOnly = true)
    public As2Config getConfigurationByClientAndInterface(Long clientId, Long interfaceId) {
        return as2ConfigRepository.findByClient_IdAndInterfaceConfig_Id(clientId, interfaceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    String.format("AS2 Configuration not found for client: %d and interface: %d", clientId, interfaceId)));
    }

    private void validateConfiguration(As2Config config) {
        if (config.getClient() == null || config.getClient().getId() == null) {
            throw new ValidationException("Client is required");
        }
        if (config.getInterfaceConfig() == null || config.getInterfaceConfig().getId() == null) {
            throw new ValidationException("Interface is required");
        }
        if (config.getServerId() == null || config.getServerId().trim().isEmpty()) {
            throw new ValidationException("Server ID is required");
        }
        if (config.getPartnerId() == null || config.getPartnerId().trim().isEmpty()) {
            throw new ValidationException("Partner ID is required");
        }
        if (config.getLocalId() == null || config.getLocalId().trim().isEmpty()) {
            throw new ValidationException("Local ID is required");
        }
    }
} 