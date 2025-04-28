package com.middleware.shared.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    private String xmlPath;

    @Column(name = "database_field", nullable = false)
    private String databaseField;

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
    private String sourceField;

    @Column
    private String targetField;

    @Column
    private String validationRule;

    @Column
    private Boolean isActive = true;

    @Column
    private String tableName;

    @Column
    private String dataType;

    @Column
    private Boolean isAttribute = false;

    @Column
    private String xsdElement;

    @Column
    private Boolean isDefault;

    @Column
    private String transformationRule;

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

    public MappingRule() {
        super();
    }

    public MappingRule(MappingRule other) {
        super();
        this.name = other.getName();
        this.xmlPath = other.getXmlPath();
        this.databaseField = other.getDatabaseField();
        this.transformation = other.getTransformation();
        this.required = other.getRequired();
        this.defaultValue = other.getDefaultValue();
        this.priority = other.getPriority();
        this.interfaceEntity = other.getInterfaceEntity();
        this.description = other.getDescription();
        this.sourceField = other.getSourceField();
        this.targetField = other.getTargetField();
        this.validationRule = other.getValidationRule();
        this.isActive = other.getIsActive();
        this.tableName = other.getTableName();
        this.dataType = other.getDataType();
        this.isAttribute = other.getIsAttribute();
        this.xsdElement = other.getXsdElement();
        this.isDefault = other.getIsDefault();
        this.transformationRule = other.getTransformationRule();
        this.client = other.getClient();
    }

    // Compatibility methods
    public String getXmlPath() {
        return xmlPath != null ? xmlPath : sourceField;
    }

    public void setXmlPath(String xmlPath) {
        this.xmlPath = xmlPath;
        if (this.sourceField == null) {
            this.sourceField = xmlPath;
        }
    }

    private String camelToSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(str.charAt(0)));
        
        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public void setDatabaseField(String databaseField) {
        // Convert to snake_case if not already in that format
        this.databaseField = camelToSnakeCase(databaseField);
        if (this.targetField == null) {
            this.targetField = this.databaseField;
        }
    }

    public boolean isAttribute() {
        return isAttribute != null ? isAttribute : false;
    }

    public void setAttribute(boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public Long getInterfaceId() {
        return interfaceEntity != null ? interfaceEntity.getId() : null;
    }

    public void setInterfaceId(Long interfaceId) {
        if (interfaceId != null) {
            this.interfaceEntity = new Interface();
            this.interfaceEntity.setId(interfaceId);
        }
    }

    public boolean isRequired() {
        return required != null ? required : false;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getClientId() {
        return client != null ? client.getId() : null;
    }

    public void setClientId(Long clientId) {
        if (clientId != null) {
            this.client = new Client();
            this.client.setId(clientId);
        }
    }
} 
