package com.middleware.shared.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Standardized MappingRule entity.
 * Uses sourceField and targetField consistently.
 * Deprecated old field names (xmlPath, databaseField) for backward compatibility.
 */
@Data
@Entity
@Table(name = "mapping_rules")
@EqualsAndHashCode(callSuper = true)
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class MappingRule extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "xml_path", nullable = false)
    private String sourceField; // Standardized field for XML path

    @Column(name = "database_field", nullable = false)
    private String targetField; // Standardized field for database field

    @Column
    private String transformation;

    @Column(name = "is_required")
    private Boolean required = false;

    @Column(name = "default_value")
    private String defaultValue;

    @Column
    private Integer priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id")
    @JsonIdentityReference(alwaysAsId = true)
    private Interface interfaceEntity;

    @Column(name = "interface_id", insertable = false, updatable = false)
    private Long interfaceId;

    @Column(length = 500)
    private String description;

    @Column
    private String validationRule;

    @Column
    private Boolean isActive = true;

    @Column
    private String tableName; // Kept for potential future use or specific scenarios

    @Column
    private String dataType; // Kept for potential future use or specific scenarios

    @Column
    private Boolean isAttribute = false; // Kept for potential future use or specific scenarios

    @Column
    private String xsdElement; // Kept for potential future use or specific scenarios

    @Column
    private Boolean isDefault;

    @Column
    private String transformationRule; // Kept for potential future use or specific scenarios

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonIdentityReference(alwaysAsId = true)
    private Client client;

    @Column(name = "client_id", insertable = false, updatable = false)
    private Long clientId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Deprecated fields for backward compatibility
    @Transient
    @Deprecated
    private String xmlPath;

    @Transient
    @Deprecated
    private String databaseField;

    public MappingRule() {
        super();
    }

    // Constructor and other methods...

    // --- Compatibility Methods (Deprecated) ---

    /**
     * @deprecated Use {@link #getSourceField()} instead.
     */
    @Deprecated
    public String getXmlPath() {
        return sourceField; // Return the standardized field
    }

    /**
     * @deprecated Use {@link #setSourceField(String)} instead.
     */
    @Deprecated
    public void setXmlPath(String xmlPath) {
        this.sourceField = xmlPath; // Set the standardized field
        this.xmlPath = xmlPath; // Keep deprecated field in sync if needed
    }

    /**
     * @deprecated Use {@link #getTargetField()} instead.
     */
    @Deprecated
    public String getDatabaseField() {
        return targetField; // Return the standardized field
    }

    /**
     * @deprecated Use {@link #setTargetField(String)} instead.
     */
    @Deprecated
    public void setDatabaseField(String databaseField) {
        this.targetField = databaseField; // Set the standardized field
        this.databaseField = databaseField; // Keep deprecated field in sync if needed
    }

    // --- Standard Getters and Setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
        // Keep deprecated field in sync if needed
        if (this.xmlPath == null) {
            this.xmlPath = sourceField;
        }
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
        // Keep deprecated field in sync if needed
        if (this.databaseField == null) {
            this.databaseField = targetField;
        }
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public Boolean getRequired() {
        return required != null ? required : false;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Interface getInterfaceEntity() {
        return interfaceEntity;
    }

    public void setInterfaceEntity(Interface interfaceEntity) {
        this.interfaceEntity = interfaceEntity;
    }

    public Long getInterfaceId() {
        return interfaceEntity != null ? interfaceEntity.getId() : interfaceId;
    }

    public void setInterfaceId(Long interfaceId) {
        this.interfaceId = interfaceId;
        if (interfaceId != null && this.interfaceEntity == null) {
            this.interfaceEntity = new Interface();
            this.interfaceEntity.setId(interfaceId);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(String validationRule) {
        this.validationRule = validationRule;
    }

    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Boolean getIsAttribute() {
        return isAttribute != null ? isAttribute : false;
    }

    public void setIsAttribute(Boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public String getXsdElement() {
        return xsdElement;
    }

    public void setXsdElement(String xsdElement) {
        this.xsdElement = xsdElement;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getTransformationRule() {
        return transformationRule;
    }

    public void setTransformationRule(String transformationRule) {
        this.transformationRule = transformationRule;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getClientId() {
        return client != null ? client.getId() : clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
        if (clientId != null && this.client == null) {
            this.client = new Client();
            this.client.setId(clientId);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

