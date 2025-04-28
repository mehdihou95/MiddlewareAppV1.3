package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.Client;
import com.middleware.processor.dto.ClientOnboardingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

/**
 * Service interface for managing Client entities.
 * Provides methods for CRUD operations and client-specific queries.
 */
public interface ClientService {
    /**
     * Retrieve all clients in the system with pagination, sorting and filtering.
     *
     * @param pageable The pagination and sorting information
     * @param nameFilter Optional filter for client name
     * @param statusFilter Optional filter for client status
     * @return Page of clients matching the criteria
     */
    Page<Client> getClients(Pageable pageable, String nameFilter, String statusFilter);

    /**
     * Find a client by their ID.
     *
     * @param id The client ID to search for
     * @return Optional containing the client if found
     */
    Optional<Client> getClientById(Long id);

    /**
     * Save a new client or update an existing one.
     *
     * @param client The client entity to save
     * @return The saved client with updated information
     */
    Client saveClient(Client client);

    /**
     * Delete a client by their ID.
     *
     * @param id The ID of the client to delete
     */
    void deleteClient(Long id);

    /**
     * Find a client by their name.
     *
     * @param name The name of the client to search for
     * @return Optional containing the client if found
     */
    Optional<Client> getClientByName(String name);

    /**
     * Check if a client with the given name exists.
     *
     * @param name The name to check
     * @return true if a client with the name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find clients by status with pagination.
     *
     * @param status The status to filter by
     * @param pageable The pagination information
     * @return Page of clients with the specified status
     */
    Page<Client> findByStatus(String status, Pageable pageable);

    /**
     * Find clients by name containing with pagination.
     *
     * @param name The name to search for
     * @param pageable The pagination information
     * @return Page of clients with names containing the search term
     */
    Page<Client> findByNameContaining(String name, Pageable pageable);

    /**
     * Find active clients with pagination.
     *
     * @param pageable The pagination information
     * @return Page of active clients
     */
    Page<Client> findActiveClients(Pageable pageable);

    /**
     * Find inactive clients with pagination.
     *
     * @param pageable The pagination information
     * @return Page of inactive clients
     */
    Page<Client> findInactiveClients(Pageable pageable);

    /**
     * Onboard a new client with the provided data.
     *
     * @param clientData The client onboarding data
     * @return The newly created client
     */
    Client onboardNewClient(ClientOnboardingDTO clientData);

    /**
     * Clone an existing client with new data.
     *
     * @param sourceClientId The ID of the client to clone
     * @param clientData The new client data
     * @return The newly created cloned client
     */
    Client cloneClient(Long sourceClientId, ClientOnboardingDTO clientData);
} 
