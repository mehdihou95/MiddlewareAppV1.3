package com.middleware.shared.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import com.middleware.shared.model.connectors.SftpConfig;

/**
 * Entity representing an interface in the system.
 * Each interface belongs to a client and has specific configuration settings.
 */
@Entity
@Table(name = "interfaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class Interface extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank(message = "Type is required")
    @Size(max = 20, message = "Type must not exceed 20 characters")
    @Column(nullable = false, length = 20)
    private String type;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Size(max = 255)
    @Column(length = 255)
    private String schemaPath;

    @NotBlank(message = "Root element is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String rootElement;

    @Size(max = 255)
    @Column(length = 255)
    private String namespace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonBackReference
    @NotNull(message = "Client is required")
    private Client client;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private int priority = 0;

    public boolean isHighPriority() {
        return priority >= 8;
    }

    public boolean isNormalPriority() {
        return priority >= 4 && priority < 8;
    }

    @OneToMany(mappedBy = "interfaceEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<MappingRule> mappingRules = new HashSet<>();

    @OneToMany(mappedBy = "interfaceEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProcessedFile> processedFiles = new HashSet<>();

    @OneToMany(mappedBy = "interfaceEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SftpConfig> sftpConfigs = new HashSet<>();

    // Getters and Setters for fields not handled by @Data
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

    public Set<SftpConfig> getSftpConfigs() {
        return sftpConfigs;
    }

    public void setSftpConfigs(Set<SftpConfig> sftpConfigs) {
        this.sftpConfigs = sftpConfigs;
    }
} 
