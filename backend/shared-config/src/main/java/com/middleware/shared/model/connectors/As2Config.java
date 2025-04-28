package com.middleware.shared.model.connectors;

import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.camel.component.as2.internal.AS2ApiName;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "as2_config")
@Data
@NoArgsConstructor
public class As2Config {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "interface_id", nullable = false)
    private Interface interfaceConfig;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "local_id", nullable = false)
    private String localId;

    @Column(name = "partner_as2_id", nullable = false)
    private String partnerAs2Id;

    @Column(name = "partner_as2_url", nullable = false)
    private String partnerAs2Url;

    @Column(name = "certificate_alias")
    private String certificateAlias;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_name", nullable = false)
    private AS2ApiName apiName = AS2ApiName.SERVER;

    @Column(name = "encryption_algorithm", nullable = false)
    private String encryptionAlgorithm = "AES256";

    @Column(name = "signature_algorithm", nullable = false)
    private String signatureAlgorithm = "SHA256";

    @Column(nullable = false)
    private boolean compression = true;

    @Column(name = "mdn_mode", nullable = false)
    private String mdnMode = "SYNC";

    @Column(name = "mdn_digest_algorithm", nullable = false)
    private String mdnDigestAlgorithm = "SHA256";

    @Column(name = "encrypt_message", nullable = false)
    private boolean encryptMessage = true;

    @Column(name = "sign_message", nullable = false)
    private boolean signMessage = true;

    @Column(name = "request_mdn", nullable = false)
    private boolean requestMdn = true;

    @Column(name = "mdn_url")
    private String mdnUrl;

    @Column(name = "retry_count")
    private Integer retryCount = 3;

    @Column(name = "retry_interval")
    private Integer retryInterval = 60;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 