package com.middleware.shared.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a client.
 */
@Entity
@Table(name = "clients")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class Client extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String code;

    @NotNull(message = "Status is required")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Size(max = 1000)
    private String description;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Interface> interfaces = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<MappingRule> mappingRules = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ProcessedFile> processedFiles = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<AsnHeader> asnHeaders = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Interface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Set<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public Set<MappingRule> getMappingRules() {
        return mappingRules;
    }

    public void setMappingRules(Set<MappingRule> mappingRules) {
        this.mappingRules = mappingRules;
    }

    public Set<ProcessedFile> getProcessedFiles() {
        return processedFiles;
    }

    public void setProcessedFiles(Set<ProcessedFile> processedFiles) {
        this.processedFiles = processedFiles;
    }

    public Set<AsnHeader> getAsnHeaders() {
        return asnHeaders;
    }

    public void setAsnHeaders(Set<AsnHeader> asnHeaders) {
        this.asnHeaders = asnHeaders;
    }

    public void setActive(boolean active) {
        this.status = active ? ClientStatus.ACTIVE : ClientStatus.INACTIVE;
    }
} 
