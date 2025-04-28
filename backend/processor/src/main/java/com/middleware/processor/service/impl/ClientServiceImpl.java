package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.ClientStatus;
import com.middleware.shared.model.connectors.SftpConfig;
import com.middleware.shared.repository.ClientRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.shared.repository.AsnHeaderRepository;
import com.middleware.shared.repository.AsnLineRepository;
import com.middleware.shared.repository.OrderHeaderRepository;
import com.middleware.shared.repository.OrderLineRepository;
import com.middleware.shared.repository.connectors.SftpConfigRepository;
import com.middleware.processor.service.interfaces.ClientService;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.processor.dto.ClientOnboardingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.middleware.shared.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ClientService with Circuit Breaker pattern.
 * Provides operations for managing clients.
 */
@Service
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ProcessedFileRepository processedFileRepository;
    private final MappingRuleRepository mappingRuleRepository;
    private final InterfaceRepository interfaceRepository;
    private final AsnHeaderRepository asnHeaderRepository;
    private final AsnLineRepository asnLineRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final OrderLineRepository orderLineRepository;
    private final SftpConfigRepository sftpConfigRepository;
    private final CircuitBreakerService circuitBreakerService;

    @Autowired
    public ClientServiceImpl(
            ClientRepository clientRepository,
            ProcessedFileRepository processedFileRepository,
            MappingRuleRepository mappingRuleRepository,
            InterfaceRepository interfaceRepository,
            AsnHeaderRepository asnHeaderRepository,
            AsnLineRepository asnLineRepository,
            OrderHeaderRepository orderHeaderRepository,
            OrderLineRepository orderLineRepository,
            SftpConfigRepository sftpConfigRepository,
            CircuitBreakerService circuitBreakerService) {
        this.clientRepository = clientRepository;
        this.processedFileRepository = processedFileRepository;
        this.mappingRuleRepository = mappingRuleRepository;
        this.interfaceRepository = interfaceRepository;
        this.asnHeaderRepository = asnHeaderRepository;
        this.asnLineRepository = asnLineRepository;
        this.orderHeaderRepository = orderHeaderRepository;
        this.orderLineRepository = orderLineRepository;
        this.sftpConfigRepository = sftpConfigRepository;
        this.circuitBreakerService = circuitBreakerService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> getClients(Pageable pageable, String nameFilter, String statusFilter) {
        log.debug("Getting clients with filters - name: {}, status: {}", nameFilter, statusFilter);
        return circuitBreakerService.<Page<Client>>executeRepositoryOperation(
            () -> {
                try {
                    if (nameFilter != null && statusFilter != null) {
                        return clientRepository.findByNameContainingIgnoreCaseAndStatus(nameFilter, statusFilter, pageable);
                    } else if (nameFilter != null) {
                        return clientRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
                    } else if (statusFilter != null) {
                        return clientRepository.findByStatus(statusFilter, pageable);
                    } else {
                        return clientRepository.findAll(pageable);
                    }
                } catch (Exception e) {
                    log.error("Error retrieving clients: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getClients");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> getClientById(Long id) {
        log.debug("Getting client by id: {}", id);
        return circuitBreakerService.<Optional<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findById(id);
                } catch (Exception e) {
                    log.error("Error retrieving client by id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for getClientById");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional
    public Client saveClient(Client client) {
        log.debug("Saving client: {}", client.getName());
        validateClient(client);
        return circuitBreakerService.<Client>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.save(client);
                } catch (Exception e) {
                    log.error("Error saving client {}: {}", client.getName(), e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting client {} to INACTIVE", client.getName());
                client.setStatus(ClientStatus.INACTIVE);
                return client;
            }
        );
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.debug("Deleting client with id: {}", id);
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    // First get the client to log its name
                    Client client = clientRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
                    
                    // Log client details before deletion
                    log.info("Starting deletion of client - Name: {}, Code: {}, ID: {}", 
                            client.getName(), client.getCode(), client.getId());

                    // Delete related records in order to avoid foreign key constraints
                    log.debug("Deleting processed files for client id: {}", id);
                    processedFileRepository.deleteByClient_Id(id);

                    log.debug("Deleting mapping rules for client id: {}", id);
                    mappingRuleRepository.deleteByClient_Id(id);

                    log.debug("Deleting SFTP configs for client id: {}", id);
                    List<SftpConfig> sftpConfigs = sftpConfigRepository.findByClient_Id(id);
                    sftpConfigs.forEach(config -> sftpConfigRepository.deleteById(config.getId()));

                    // Delete ASN records
                    log.debug("Deleting ASN lines for client id: {}", id);
                    asnLineRepository.deleteByClient_Id(id);

                    log.debug("Deleting ASN headers for client id: {}", id);
                    asnHeaderRepository.deleteByClient_Id(id);

                    // Delete Order records
                    log.debug("Deleting Order lines for client id: {}", id);
                    orderLineRepository.deleteByClient_Id(id);

                    log.debug("Deleting Order headers for client id: {}", id);
                    orderHeaderRepository.deleteByClient_Id(id);

                    // Delete interfaces
                    log.debug("Deleting interfaces for client id: {}", id);
                    interfaceRepository.deleteByClient_Id(id);

                    // Finally delete the client
                    log.debug("Deleting client record with id: {}", id);
                    clientRepository.deleteById(id);

                    // Log successful deletion
                    log.info("Successfully deleted client - Name: {}, Code: {}, ID: {}", 
                            client.getName(), client.getCode(), client.getId());
                } catch (Exception e) {
                    log.error("Error deleting client with id {}: {}", id, e.getMessage(), e);
                    throw new ValidationException("Failed to delete client: " + e.getMessage());
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to delete client with id {}", id);
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> getClientByName(String name) {
        log.debug("Getting client by name: {}", name);
        return circuitBreakerService.<Optional<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findByName(name);
                } catch (Exception e) {
                    log.error("Error retrieving client by name {}: {}", name, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for getClientByName");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        log.debug("Checking if client exists by name: {}", name);
        return circuitBreakerService.<Boolean>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.existsByName(name);
                } catch (Exception e) {
                    log.error("Error checking client existence by name {}: {}", name, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning false for existsByName");
                return false;
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> findByStatus(String status, Pageable pageable) {
        log.debug("Finding clients by status: {}", status);
        return circuitBreakerService.<Page<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findByStatus(status, pageable);
                } catch (Exception e) {
                    log.error("Error finding clients by status {}: {}", status, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findByStatus");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> findByNameContaining(String name, Pageable pageable) {
        log.debug("Finding clients by name containing: {}", name);
        return circuitBreakerService.<Page<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findByNameContainingIgnoreCase(name, pageable);
                } catch (Exception e) {
                    log.error("Error finding clients by name containing {}: {}", name, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findByNameContaining");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> findActiveClients(Pageable pageable) {
        log.debug("Finding active clients");
        return circuitBreakerService.<Page<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findByStatus(ClientStatus.ACTIVE.name(), pageable);
                } catch (Exception e) {
                    log.error("Error finding active clients: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findActiveClients");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> findInactiveClients(Pageable pageable) {
        log.debug("Finding inactive clients");
        return circuitBreakerService.<Page<Client>>executeRepositoryOperation(
            () -> {
                try {
                    return clientRepository.findByStatus(ClientStatus.INACTIVE.name(), pageable);
                } catch (Exception e) {
                    log.error("Error finding inactive clients: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findInactiveClients");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional
    public Client onboardNewClient(ClientOnboardingDTO clientData) {
        log.debug("Onboarding new client: {}", clientData.getName());
        Client client = new Client();
        client.setName(clientData.getName());
        client.setDescription(clientData.getDescription());
        client.setStatus(clientData.getActive() ? ClientStatus.ACTIVE : ClientStatus.INACTIVE);
        return saveClient(client);
    }

    @Override
    @Transactional
    public Client cloneClient(Long sourceClientId, ClientOnboardingDTO clientData) {
        log.debug("Cloning client {} with new data: {}", sourceClientId, clientData.getName());
        return circuitBreakerService.<Client>executeRepositoryOperation(
            () -> {
                try {
                    Client sourceClient = clientRepository.findById(sourceClientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Source client not found with id: " + sourceClientId));
                    
                    Client newClient = new Client();
                    newClient.setName(clientData.getName());
                    newClient.setDescription(clientData.getDescription());
                    newClient.setStatus(clientData.getActive() ? ClientStatus.ACTIVE : ClientStatus.INACTIVE);
                    
                    return clientRepository.save(newClient);
                } catch (Exception e) {
                    log.error("Error cloning client {}: {}", sourceClientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Creating inactive client for clone operation");
                Client errorClient = new Client();
                errorClient.setName(clientData.getName());
                errorClient.setStatus(ClientStatus.INACTIVE);
                return errorClient;
            }
        );
    }

    private void validateClient(Client client) {
        if (client.getName() == null || client.getName().trim().isEmpty()) {
            throw new ValidationException("Client name is required");
        }
        if (client.getCode() == null || client.getCode().trim().isEmpty()) {
            throw new ValidationException("Client code is required");
        }
        if (clientRepository.existsByNameAndIdNot(client.getName(), client.getId())) {
            throw new ValidationException("Client name already exists");
        }
        if (clientRepository.existsByCode(client.getCode())) {
            throw new ValidationException("Client code already exists");
        }
    }
}
