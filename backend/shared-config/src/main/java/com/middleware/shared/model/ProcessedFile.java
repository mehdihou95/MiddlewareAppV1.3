package com.middleware.shared.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.Type;

/**
 * Entity representing a processed file.
 */
@Entity
@Table(name = "processed_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"
)
public class ProcessedFile extends BaseEntity {

    @NotBlank(message = "File name is required")
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String fileName;

    @NotBlank(message = "Status is required")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String status;

    @Size(max = 1000)
    @Column(length = 1000)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String content;

    @NotNull(message = "Interface is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Interface interfaceEntity;

    @NotNull(message = "Client is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Client client;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column
    private String filePath; // Reference to actual file storage

    @Column
    private String storageType; // "DB" or "FS"

    @Lob
    @Column
    private byte[] contentBytes; // Only used if storageType is "DB"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asn_id")
    @JsonBackReference
    private AsnHeader asnHeader;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
