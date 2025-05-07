package com.middleware.processor.controller;
    
import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import com.middleware.processor.service.interfaces.ClientOnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
    
import java.util.List;
    
@RestController
@RequestMapping("/api/clients/onboarding")
public class ClientOnboardingController {
    
    private final ClientOnboardingService clientOnboardingService;
    
    public ClientOnboardingController(ClientOnboardingService clientOnboardingService) {
        this.clientOnboardingService = clientOnboardingService;
    }
    
    @PostMapping("/new")
    public ResponseEntity<Client> onboardNewClient(@RequestBody Client client) {
        List<MappingRule> defaultRules = clientOnboardingService.getDefaultMappingRules();
        return ResponseEntity.ok(clientOnboardingService.onboardNewClient(client, defaultRules));
    }
    
    @PostMapping("/clone/{sourceClientId}")
    public ResponseEntity<Client> cloneClientConfiguration(
            @PathVariable Long sourceClientId,
            @RequestBody Client newClient) {
        return ResponseEntity.ok(clientOnboardingService.cloneClientConfiguration(sourceClientId, newClient));
    }
    
    @GetMapping("/default-rules")
    public ResponseEntity<List<MappingRule>> getDefaultMappingRules() {
        return ResponseEntity.ok(clientOnboardingService.getDefaultMappingRules());
    }
} 
