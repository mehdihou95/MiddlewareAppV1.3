package com.middleware.listener.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SftpConfigDTO {
    private Long id;
    private Long clientId;
    private Long interfaceId;
    private String host;
    private Integer port = 22;
    private String username;
    private String password;
    private String privateKeyPath;
    private String privateKeyPassphrase;
    private String remoteDirectory;
    private List<String> monitoredDirectories;
    private String filePattern;
    private Integer connectionTimeout = 5000;
    private Integer channelTimeout = 30000;
    private Integer threadPoolSize = 4;
    private Integer retryAttempts = 3;
    private Integer retryDelay = 5000;
    private Integer pollingInterval = 60000;
    private boolean active = true;
} 