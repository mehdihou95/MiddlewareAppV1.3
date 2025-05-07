package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.exception.ResourceNotFoundException;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.connectors.SftpConfig;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.shared.repository.connectors.SftpConfigRepository;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.processor.service.interfaces.InterfaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.StringReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Implementation of InterfaceService with Circuit Breaker pattern.
 * Provides operations for managing interfaces.
 */
@Service
@Slf4j
public class InterfaceServiceImpl implements InterfaceService {

    private final InterfaceRepository interfaceRepository;
    private final ProcessedFileRepository processedFileRepository;
    private final MappingRuleRepository mappingRuleRepository;
    private final SftpConfigRepository sftpConfigRepository;
    private final CircuitBreakerService circuitBreakerService;
    
    @Autowired
    public InterfaceServiceImpl(
            InterfaceRepository interfaceRepository,
            ProcessedFileRepository processedFileRepository,
            MappingRuleRepository mappingRuleRepository,
            SftpConfigRepository sftpConfigRepository,
            CircuitBreakerService circuitBreakerService) {
        this.interfaceRepository = interfaceRepository;
        this.processedFileRepository = processedFileRepository;
        this.mappingRuleRepository = mappingRuleRepository;
        this.sftpConfigRepository = sftpConfigRepository;
        this.circuitBreakerService = circuitBreakerService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interface> getAllInterfaces() {
        log.debug("Retrieving all interfaces");
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findAll();
                } catch (Exception e) {
                    log.error("Error retrieving all interfaces: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getAllInterfaces");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getAllInterfaces(Pageable pageable) {
        log.debug("Retrieving all interfaces with pagination");
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findAll(pageable);
                } catch (Exception e) {
                    log.error("Error retrieving all interfaces with pagination: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getAllInterfaces");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Interface> getInterfaceById(Long id) {
        log.debug("Retrieving interface by id: {}", id);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findById(id);
                } catch (Exception e) {
                    log.error("Error retrieving interface by id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for getInterfaceById");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interface> getInterfacesByClient(Client client) {
        log.debug("Retrieving interfaces for client: {}", client.getName());
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByClient(client);
                } catch (Exception e) {
                    log.error("Error retrieving interfaces for client {}: {}", client.getName(), e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getInterfacesByClient");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Interface> getClientInterfaces(Long clientId) {
        log.debug("Retrieving interfaces for client id: {}", clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByClient_Id(clientId);
                } catch (Exception e) {
                    log.error("Error retrieving interfaces for client id {}: {}", clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getClientInterfaces");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfacesByClient(Long clientId, Pageable pageable) {
        log.debug("Retrieving interfaces for client id {} with pagination", clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByClient_Id(clientId, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving interfaces for client id {} with pagination: {}", clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getInterfacesByClient");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional
    public Interface createInterface(Interface interfaceEntity) {
        log.debug("Creating new interface: {}", interfaceEntity.getName());
        validateInterface(interfaceEntity);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.save(interfaceEntity);
                } catch (Exception e) {
                    log.error("Error creating interface {}: {}", interfaceEntity.getName(), e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting interface {} to inactive", interfaceEntity.getName());
                interfaceEntity.setActive(false);
                return interfaceEntity;
            }
        );
    }

    @Override
    @Transactional
    public Interface updateInterface(Long id, Interface interfaceEntity) {
        log.debug("Updating interface with id: {}", id);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    Interface existingInterface = interfaceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + id));
                    
                    validateInterface(interfaceEntity);
                    
                    existingInterface.setName(interfaceEntity.getName());
                    existingInterface.setType(interfaceEntity.getType());
                    existingInterface.setDescription(interfaceEntity.getDescription());
                    existingInterface.setSchemaPath(interfaceEntity.getSchemaPath());
                    existingInterface.setRootElement(interfaceEntity.getRootElement());
                    existingInterface.setNamespace(interfaceEntity.getNamespace());
                    existingInterface.setActive(interfaceEntity.isActive());
                    existingInterface.setPriority(interfaceEntity.getPriority());
                    existingInterface.setClient(interfaceEntity.getClient());
                    
                    return interfaceRepository.save(existingInterface);
                } catch (Exception e) {
                    log.error("Error updating interface with id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting interface {} to inactive", interfaceEntity.getName());
                interfaceEntity.setActive(false);
                return interfaceEntity;
            }
        );
    }

    @Override
    @Transactional
    public void deleteInterface(Long id) {
        log.debug("Deleting interface with id: {}", id);
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    if (!interfaceRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Interface not found with id: " + id);
                    }

                    // Delete related records in order to avoid foreign key constraints
                    log.debug("Deleting processed files for interface id: {}", id);
                    processedFileRepository.deleteByInterfaceId(id);

                    log.debug("Deleting mapping rules for interface id: {}", id);
                    mappingRuleRepository.deleteByInterfaceId(id);

                    log.debug("Deleting SFTP configs for interface id: {}", id);
                    List<SftpConfig> sftpConfigs = sftpConfigRepository.findByInterfaceEntity_Id(id);
                    sftpConfigs.forEach(config -> sftpConfigRepository.deleteById(config.getId()));

                    // Finally delete the interface
                    log.debug("Deleting interface record with id: {}", id);
                    interfaceRepository.deleteById(id);
                } catch (Exception e) {
                    log.error("Error deleting interface with id {}: {}", id, e.getMessage(), e);
                    throw new ValidationException("Failed to delete interface: " + e.getMessage());
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to delete interface with id {}", id);
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Interface> getInterfaceByName(String name, Long clientId) {
        log.debug("Retrieving interface by name {} for client id {}", name, clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByNameAndClient_Id(name, clientId);
                } catch (Exception e) {
                    log.error("Error retrieving interface by name {} for client id {}: {}", name, clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for getInterfaceByName");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfacesByClient(Long clientId, int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving interfaces for client id {} with pagination and sorting", clientId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return getInterfacesByClient(clientId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfaces(int page, int size, String sortBy, String sortDirection, String searchTerm, Boolean isActive) {
        log.debug("Retrieving interfaces with filters - searchTerm: {}, isActive: {}", searchTerm, isActive);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return searchInterfaces(searchTerm, pageable);
        }
        
        if (isActive != null) {
            return getInterfacesByStatus(isActive, pageable);
        }
        
        return getAllInterfaces(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> searchInterfaces(String name, Pageable pageable) {
        log.debug("Searching interfaces by name: {}", name);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByNameContaining(name, pageable);
                } catch (Exception e) {
                    log.error("Error searching interfaces by name {}: {}", name, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for searchInterfaces");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> searchInterfaces(String searchTerm, int page, int size, String sortBy, String sortDirection) {
        log.debug("Searching interfaces with pagination and sorting");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return searchInterfaces(searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfacesByType(String type, int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving interfaces by type: {}", type);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByType(type, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving interfaces by type {}: {}", type, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getInterfacesByType");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfacesByStatus(boolean isActive, Pageable pageable) {
        log.debug("Retrieving interfaces by status: {}", isActive);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.findByIsActive(isActive, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving interfaces by status {}: {}", isActive, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getInterfacesByStatus");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Interface> getInterfacesByStatus(boolean isActive, int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving interfaces by status with pagination and sorting");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return getInterfacesByStatus(isActive, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Interface detectInterface(String xmlContent, Long clientId) {
        log.debug("Detecting interface for client id: {}", clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    if (xmlContent == null || xmlContent.trim().isEmpty()) {
                        throw new ValidationException("XML content cannot be empty");
                    }

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

                    Element rootElement = doc.getDocumentElement();
                    String rootElementName = rootElement.getTagName();

                    List<Interface> interfaces = interfaceRepository.findByClient_Id(clientId);
                    return interfaces.stream()
                        .filter(i -> i.getRootElement().equals(rootElementName))
                        .findFirst()
                        .orElseThrow(() -> new ValidationException("No matching interface found for XML content"));
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    log.error("Error parsing XML content for client id {}: {}", clientId, e.getMessage(), e);
                    throw new ValidationException("Failed to parse XML content: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Error detecting interface for client id {}: {}", clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Throwing validation exception for detectInterface");
                throw new ValidationException("Failed to detect interface type");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndClientId(String name, Long clientId) {
        log.debug("Checking if interface exists by name {} for client id {}", name, clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return interfaceRepository.existsByNameAndClient_Id(name, clientId);
                } catch (Exception e) {
                    log.error("Error checking interface existence by name {} for client id {}: {}", name, clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning false for existsByNameAndClientId");
                return false;
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MappingRule> getInterfaceMappings(Long interfaceId) {
        log.debug("Retrieving mappings for interface id: {}", interfaceId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + interfaceId));
                    return new ArrayList<>(interfaceEntity.getMappingRules());
                } catch (Exception e) {
                    log.error("Error retrieving mappings for interface id {}: {}", interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getInterfaceMappings");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional
    public List<MappingRule> updateInterfaceMappings(Long interfaceId, List<MappingRule> mappings) {
        log.debug("Updating mappings for interface id: {}", interfaceId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + interfaceId));
                    
                    interfaceEntity.getMappingRules().clear();
                    if (mappings != null) {
                        mappings.forEach(mapping -> {
                            mapping.setInterfaceEntity(interfaceEntity);
                            interfaceEntity.getMappingRules().add(mapping);
                        });
                    }
                    
                    interfaceRepository.save(interfaceEntity);
                    return new ArrayList<>(interfaceEntity.getMappingRules());
                } catch (Exception e) {
                    log.error("Error updating mappings for interface id {}: {}", interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for updateInterfaceMappings");
                return new ArrayList<>();
            }
        );
    }

    private void validateInterface(Interface interfaceEntity) {
        if (interfaceEntity.getName() == null || interfaceEntity.getName().trim().isEmpty()) {
            throw new ValidationException("Interface name is required");
        }
        if (interfaceEntity.getType() == null || interfaceEntity.getType().trim().isEmpty()) {
            throw new ValidationException("Interface type is required");
        }
        if (interfaceEntity.getRootElement() == null || interfaceEntity.getRootElement().trim().isEmpty()) {
            throw new ValidationException("Root element is required");
        }
        if (interfaceEntity.getClient() == null) {
            throw new ValidationException("Client is required");
        }
    }
}
