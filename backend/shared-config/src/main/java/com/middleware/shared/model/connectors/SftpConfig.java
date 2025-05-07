package com.middleware.shared.model.connectors;

import com.fasterxml.jackson.annotation.*;
import com.middleware.shared.model.BaseEntity;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sftp_config", uniqueConstraints = {
    @UniqueConstraint(name = "unique_client_interface", columnNames = {"client_id", "interface_id"})
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id",
    scope = SftpConfig.class
)
public class SftpConfig extends BaseEntity {

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port = 22;

    @Column(nullable = false)
    private String username;

    private String password;

    @Column(name = "private_key")
    private String privateKey;

    @Column(name = "private_key_passphrase")
    private String privateKeyPassphrase;

    @Column(name = "remote_directory", nullable = false)
    private String remoteDirectory;

    @Column(name = "file_pattern")
    private String filePattern;

    @Column(name = "monitored_directories")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> monitoredDirectories = new ArrayList<>();

    @Column(name = "processed_directory")
    private String processedDirectory;

    @Column(name = "error_directory")
    private String errorDirectory;

    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 30000;

    @Column(name = "channel_timeout")
    private Integer channelTimeout = 15000;

    @Column(name = "thread_pool_size")
    private Integer threadPoolSize = 4;

    @Column(name = "retry_attempts")
    private Integer retryAttempts = 3;

    @Column(name = "retry_delay")
    private Integer retryDelay = 5000;

    @Column(name = "polling_interval")
    private Integer pollingInterval = 60000;

    @Column(name = "is_active")
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private Client client;

    @Column(name = "client_id", insertable = false, updatable = false)
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty("interfaceConfig")
    private Interface interfaceEntity;

    @Column(name = "interface_id", insertable = false, updatable = false)
    private Long interfaceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SftpConfig(SftpConfig other) {
        super();
        this.host = other.getHost();
        this.port = other.getPort();
        this.username = other.getUsername();
        this.password = other.getPassword();
        this.privateKey = other.getPrivateKey();
        this.privateKeyPassphrase = other.getPrivateKeyPassphrase();
        this.remoteDirectory = other.getRemoteDirectory();
        this.filePattern = other.getFilePattern();
        this.monitoredDirectories = new ArrayList<>(other.getMonitoredDirectories());
        this.processedDirectory = other.getProcessedDirectory();
        this.errorDirectory = other.getErrorDirectory();
        this.connectionTimeout = other.getConnectionTimeout();
        this.channelTimeout = other.getChannelTimeout();
        this.threadPoolSize = other.getThreadPoolSize();
        this.retryAttempts = other.getRetryAttempts();
        this.retryDelay = other.getRetryDelay();
        this.pollingInterval = other.getPollingInterval();
        this.active = other.isActive();
        this.client = other.getClient();
        this.interfaceEntity = other.getInterfaceEntity();
    }

    // Getters and setters for relationship fields
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

    @JsonProperty("interfaceConfig")
    public Interface getInterfaceEntity() {
        return interfaceEntity;
    }

    @JsonProperty("interfaceConfig")
    public void setInterfaceEntity(Interface interfaceEntity) {
        this.interfaceEntity = interfaceEntity;
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

    public boolean isActive() {
        return active != null ? active : false;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
