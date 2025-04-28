package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Service interface for client onboarding operations.
 */
public interface ClientOnboardingService {
    /**
     * Get the default mapping rules for new clients.
     *
     * @return List of default mapping rules
     */
    List<MappingRule> getDefaultMappingRules();

    /**
     * Onboard a new client with default mapping rules.
     *
     * @param client The client to onboard
     * @param defaultRules The default mapping rules to apply
     * @return The onboarded client
     */
    Client onboardNewClient(Client client, List<MappingRule> defaultRules);

    /**
     * Clone configuration from an existing client to a new client.
     *
     * @param sourceClientId The ID of the client to clone from
     * @param newClient The new client to clone to
     * @return The new client with cloned configuration
     */
    Client cloneClientConfiguration(Long sourceClientId, Client newClient);

    /**
     * Onboards a new client with configuration files.
     */
    Client onboardClient(Client client, MultipartFile[] configFiles);

    /**
     * Updates an existing client's configuration.
     */
    Client updateClientConfiguration(Long clientId, MultipartFile[] configFiles);

    /**
     * Validates client configuration files.
     */
    boolean validateClientConfiguration(MultipartFile[] configFiles);

    /**
     * Processes client configuration files.
     */
    void processClientConfiguration(Long clientId, MultipartFile[] configFiles);
} 
