package com.middleware.listener.connectors.as2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Configuration
@ConfigurationProperties(prefix = "as2")
public class As2Properties {
    @NotNull
    private String serverId;

    @NotNull
    private String serverHost;

    @Positive
    private int serverPort = 8080;

    @NotNull
    private String keyStorePath;

    @NotNull
    private String keyStorePassword;

    @NotNull
    private String privateKeyAlias;

    @NotNull
    private String privateKeyPassword;

    private String[] partnerCertificates;

    private boolean requestSignature = true;

    private boolean requestEncryption = true;

    private boolean requestMdn = true;

    private boolean mdnSignature = true;

    @NotNull
    private String processedDirectory = "processed";

    @NotNull
    private String errorDirectory = "error";

    @Positive
    private int maxRetries = 3;

    @Positive
    private long retryDelay = 5000;

    // Getters and Setters
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    public String getKeyStorePath() { return keyStorePath; }
    public void setKeyStorePath(String keyStorePath) { this.keyStorePath = keyStorePath; }

    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }

    public String getPrivateKeyAlias() { return privateKeyAlias; }
    public void setPrivateKeyAlias(String privateKeyAlias) { this.privateKeyAlias = privateKeyAlias; }

    public String getPrivateKeyPassword() { return privateKeyPassword; }
    public void setPrivateKeyPassword(String privateKeyPassword) { this.privateKeyPassword = privateKeyPassword; }

    public String[] getPartnerCertificates() { return partnerCertificates; }
    public void setPartnerCertificates(String[] partnerCertificates) { this.partnerCertificates = partnerCertificates; }

    public boolean isRequestSignature() { return requestSignature; }
    public void setRequestSignature(boolean requestSignature) { this.requestSignature = requestSignature; }

    public boolean isRequestEncryption() { return requestEncryption; }
    public void setRequestEncryption(boolean requestEncryption) { this.requestEncryption = requestEncryption; }

    public boolean isRequestMdn() { return requestMdn; }
    public void setRequestMdn(boolean requestMdn) { this.requestMdn = requestMdn; }

    public boolean isMdnSignature() { return mdnSignature; }
    public void setMdnSignature(boolean mdnSignature) { this.mdnSignature = mdnSignature; }

    public String getProcessedDirectory() { return processedDirectory; }
    public void setProcessedDirectory(String processedDirectory) { this.processedDirectory = processedDirectory; }

    public String getErrorDirectory() { return errorDirectory; }
    public void setErrorDirectory(String errorDirectory) { this.errorDirectory = errorDirectory; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getRetryDelay() { return retryDelay; }
    public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
} 