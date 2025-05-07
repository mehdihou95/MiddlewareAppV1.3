package com.middleware.listener.connectors.as2.controller;

import com.middleware.shared.model.connectors.As2Config;
import com.middleware.listener.connectors.as2.service.As2ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/as2/config")
@RequiredArgsConstructor
public class As2ConfigController {
    private final As2ConfigService as2ConfigService;

    @GetMapping
    public ResponseEntity<List<As2Config>> getAllConfigurations() {
        return ResponseEntity.ok(as2ConfigService.getAllConfigurations());
    }

    @GetMapping("/active")
    public ResponseEntity<List<As2Config>> getActiveConfigurations() {
        return ResponseEntity.ok(as2ConfigService.getActiveConfigurations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<As2Config> getConfiguration(@PathVariable Long id) {
        return ResponseEntity.ok(as2ConfigService.getConfiguration(id));
    }

    @PostMapping
    public ResponseEntity<As2Config> createConfiguration(@Valid @RequestBody As2Config config) {
        return ResponseEntity.ok(as2ConfigService.createConfiguration(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<As2Config> updateConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody As2Config config) {
        return ResponseEntity.ok(as2ConfigService.updateConfiguration(id, config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        as2ConfigService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<As2Config> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(as2ConfigService.toggleActive(id));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<As2Config>> getConfigurationsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(as2ConfigService.getConfigurationsByClient(clientId));
    }

    @GetMapping("/interface/{interfaceId}")
    public ResponseEntity<List<As2Config>> getConfigurationsByInterface(@PathVariable Long interfaceId) {
        return ResponseEntity.ok(as2ConfigService.getConfigurationsByInterface(interfaceId));
    }

    @GetMapping("/client/{clientId}/interface/{interfaceId}")
    public ResponseEntity<As2Config> getConfigurationByClientAndInterface(
            @PathVariable Long clientId,
            @PathVariable Long interfaceId) {
        return ResponseEntity.ok(as2ConfigService.getConfigurationByClientAndInterface(clientId, interfaceId));
    }
} 