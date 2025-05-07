package com.middleware.listener.mapper;

import com.middleware.listener.dto.SftpConfigDTO;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.connectors.SftpConfig;
import org.springframework.stereotype.Component;

@Component
public class SftpConfigMapper {
    
    public SftpConfigDTO toDTO(SftpConfig entity) {
        if (entity == null) {
            return null;
        }

        SftpConfigDTO dto = new SftpConfigDTO();
        dto.setId(entity.getId());
        dto.setClientId(entity.getClientId());
        dto.setInterfaceId(entity.getInterfaceId());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        dto.setPrivateKeyPath(entity.getPrivateKey());
        dto.setPrivateKeyPassphrase(entity.getPrivateKeyPassphrase());
        dto.setRemoteDirectory(entity.getRemoteDirectory());
        dto.setMonitoredDirectories(entity.getMonitoredDirectories());
        dto.setFilePattern(entity.getFilePattern());
        dto.setConnectionTimeout(entity.getConnectionTimeout());
        dto.setChannelTimeout(entity.getChannelTimeout());
        dto.setThreadPoolSize(entity.getThreadPoolSize());
        dto.setRetryAttempts(entity.getRetryAttempts());
        dto.setRetryDelay(entity.getRetryDelay());
        dto.setPollingInterval(entity.getPollingInterval());
        dto.setActive(entity.isActive());
        
        return dto;
    }

    public SftpConfig toEntity(SftpConfigDTO dto) {
        if (dto == null) {
            return null;
        }

        SftpConfig entity = new SftpConfig();
        entity.setId(dto.getId());
        
        // Set client reference
        if (dto.getClientId() != null) {
            Client client = new Client();
            client.setId(dto.getClientId());
            entity.setClient(client);
        }

        // Set interface reference
        if (dto.getInterfaceId() != null) {
            Interface interfaceEntity = new Interface();
            interfaceEntity.setId(dto.getInterfaceId());
            entity.setInterfaceEntity(interfaceEntity);
        }

        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setPrivateKey(dto.getPrivateKeyPath());
        entity.setPrivateKeyPassphrase(dto.getPrivateKeyPassphrase());
        entity.setRemoteDirectory(dto.getRemoteDirectory());
        entity.setMonitoredDirectories(dto.getMonitoredDirectories());
        entity.setFilePattern(dto.getFilePattern());
        entity.setConnectionTimeout(dto.getConnectionTimeout());
        entity.setChannelTimeout(dto.getChannelTimeout());
        entity.setThreadPoolSize(dto.getThreadPoolSize());
        entity.setRetryAttempts(dto.getRetryAttempts());
        entity.setRetryDelay(dto.getRetryDelay());
        entity.setPollingInterval(dto.getPollingInterval());
        entity.setActive(dto.isActive());

        return entity;
    }

    public void updateEntity(SftpConfig entity, SftpConfigDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        // Update client reference if changed
        if (dto.getClientId() != null && !dto.getClientId().equals(entity.getClientId())) {
            Client client = new Client();
            client.setId(dto.getClientId());
            entity.setClient(client);
        }

        // Update interface reference if changed
        if (dto.getInterfaceId() != null && !dto.getInterfaceId().equals(entity.getInterfaceId())) {
            Interface interfaceEntity = new Interface();
            interfaceEntity.setId(dto.getInterfaceId());
            entity.setInterfaceEntity(interfaceEntity);
        }

        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setPrivateKey(dto.getPrivateKeyPath());
        entity.setPrivateKeyPassphrase(dto.getPrivateKeyPassphrase());
        entity.setRemoteDirectory(dto.getRemoteDirectory());
        entity.setMonitoredDirectories(dto.getMonitoredDirectories());
        entity.setFilePattern(dto.getFilePattern());
        entity.setConnectionTimeout(dto.getConnectionTimeout());
        entity.setChannelTimeout(dto.getChannelTimeout());
        entity.setThreadPoolSize(dto.getThreadPoolSize());
        entity.setRetryAttempts(dto.getRetryAttempts());
        entity.setRetryDelay(dto.getRetryDelay());
        entity.setPollingInterval(dto.getPollingInterval());
        entity.setActive(dto.isActive());
    }
} 