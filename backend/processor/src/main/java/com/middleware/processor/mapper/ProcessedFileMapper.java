package com.middleware.processor.mapper;

import com.middleware.processor.dto.ProcessedFileDTO;
import com.middleware.shared.model.ProcessedFile;
import org.springframework.stereotype.Component;
import org.hibernate.Hibernate;

@Component
public class ProcessedFileMapper {
    
    public ProcessedFileDTO toDTO(ProcessedFile processedFile) {
        if (processedFile == null) {
            return null;
        }

        ProcessedFileDTO dto = new ProcessedFileDTO();
        dto.setId(processedFile.getId());
        dto.setFileName(processedFile.getFileName());
        dto.setStatus(processedFile.getStatus());
        dto.setErrorMessage(processedFile.getErrorMessage());
        dto.setProcessedAt(processedFile.getProcessedAt());
        dto.setCreatedAt(processedFile.getCreatedAt());

        // Safely handle client information with Hibernate.initialize
        if (processedFile.getClient() != null) {
            Hibernate.initialize(processedFile.getClient());
            dto.setClientId(processedFile.getClient().getId());
            dto.setClientName(processedFile.getClient().getName());
        }

        // Safely handle interface information with Hibernate.initialize
        if (processedFile.getInterfaceEntity() != null) {
            Hibernate.initialize(processedFile.getInterfaceEntity());
            dto.setInterfaceId(processedFile.getInterfaceEntity().getId());
            dto.setInterfaceName(processedFile.getInterfaceEntity().getName());
        }

        return dto;
    }
} 
