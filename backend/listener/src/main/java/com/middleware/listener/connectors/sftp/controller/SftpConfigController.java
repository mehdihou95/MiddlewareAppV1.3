package com.middleware.listener.connectors.sftp.controller;

import com.middleware.listener.dto.SftpConfigDTO;
import com.middleware.listener.mapper.SftpConfigMapper;
import com.middleware.listener.connectors.sftp.service.SftpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sftp/config")
@RequiredArgsConstructor
public class SftpConfigController {
    private final SftpConfigService sftpConfigService;
    private final SftpConfigMapper sftpConfigMapper;

    @GetMapping
    public ResponseEntity<List<SftpConfigDTO>> getAllConfigurations() {
        return ResponseEntity.ok(sftpConfigService.getAllConfigurations().stream()
                .map(sftpConfigMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<SftpConfigDTO>> getActiveConfigurations() {
        return ResponseEntity.ok(sftpConfigService.getActiveConfigurations().stream()
                .map(sftpConfigMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SftpConfigDTO> getConfiguration(@PathVariable Long id) {
        return ResponseEntity.ok(sftpConfigMapper.toDTO(sftpConfigService.getConfiguration(id)));
    }

    @PostMapping
    public ResponseEntity<SftpConfigDTO> createConfiguration(@Valid @RequestBody SftpConfigDTO configDTO) {
        validateFilePattern(configDTO);
        return ResponseEntity.ok(sftpConfigMapper.toDTO(
            sftpConfigService.createConfiguration(sftpConfigMapper.toEntity(configDTO))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SftpConfigDTO> updateConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody SftpConfigDTO configDTO) {
        validateFilePattern(configDTO);
        return ResponseEntity.ok(sftpConfigMapper.toDTO(
            sftpConfigService.updateConfiguration(id, sftpConfigMapper.toEntity(configDTO))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        sftpConfigService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<SftpConfigDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(sftpConfigMapper.toDTO(sftpConfigService.toggleActive(id)));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<SftpConfigDTO>> getConfigurationsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(sftpConfigService.getConfigurationsByClient(clientId).stream()
                .map(sftpConfigMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/interface/{interfaceId}")
    public ResponseEntity<List<SftpConfigDTO>> getConfigurationsByInterface(@PathVariable Long interfaceId) {
        return ResponseEntity.ok(sftpConfigService.getConfigurationsByInterface(interfaceId).stream()
                .map(sftpConfigMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/client/{clientId}/interface/{interfaceId}")
    public ResponseEntity<SftpConfigDTO> getConfigurationByClientAndInterface(
            @PathVariable Long clientId,
            @PathVariable Long interfaceId) {
        return ResponseEntity.ok(sftpConfigMapper.toDTO(
            sftpConfigService.getConfigurationByClientAndInterface(clientId, interfaceId)));
    }

    private void validateFilePattern(SftpConfigDTO configDTO) {
        if (configDTO.getFilePattern() == null || configDTO.getFilePattern().trim().isEmpty()) {
            throw new IllegalArgumentException("File pattern is required");
        }
    }
} 