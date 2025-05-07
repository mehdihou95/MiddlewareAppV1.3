package com.middleware.listener.connectors.sftp.config;

import com.middleware.shared.model.connectors.SftpConfig;
import com.middleware.listener.connectors.sftp.service.SftpConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Exchange;
import org.apache.camel.component.file.remote.RemoteFileEndpoint;
import org.apache.camel.component.file.remote.RemoteFileOperations;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class SftpCamelConfig {
    private final SftpConfigService sftpConfigService;
    private final CamelContext camelContext;

    @PostConstruct
    public void initializeRoutes() {
        updateRoutes();
        initializeInboundRoute();
    }

    @Scheduled(fixedDelay = 60000) // Check for config changes every minute
    public void updateRoutes() {
        try {
            List<SftpConfig> activeConfigs = sftpConfigService.getActiveConfigurations();
            
            // Remove inactive routes
            removeInactiveRoutes(activeConfigs);
            
            // Add or update routes for active configs
            for (SftpConfig config : activeConfigs) {
                try {
                    createOrUpdateRoute(config);
                } catch (Exception e) {
                    log.error("Failed to create/update route for config: {} - {}", config.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error updating SFTP routes: {}", e.getMessage());
        }
    }

    private void handleSftpError(Exception e, String context, SftpConfig config) {
        log.error("SFTP Error in {} for config {}: {}", context, config.getId(), e.getMessage());
        log.debug("Error details: ", e);
        throw new RuntimeException(String.format("Failed to %s: %s", context, e.getMessage()), e);
    }

    @SuppressWarnings("unchecked")
    private void testSftpConnection(String routeId, String uri, SftpConfig config) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("timer://sftpConnectionTest?repeatCount=1&period=1000")
                        .routeId(routeId + "-connection-test")
                        .process(exchange -> {
                            log.info("Testing SFTP connection to {}:{}", config.getHost(), config.getPort());
                            try {
                                RemoteFileEndpoint<?> endpoint = (RemoteFileEndpoint<?>) getContext().getEndpoint(uri);
                                RemoteFileOperations<?> operations = endpoint.createRemoteFileOperations();
                                operations.connect(endpoint.getConfiguration(), exchange);
                                log.info("Successfully connected to SFTP server");
                                
                                // Test directory access
                                String testPath = normalizePath(config.getRemoteDirectory(), config);
                                try {
                                    operations.listFiles(testPath);
                                    log.info("Successfully accessed directory: {}", testPath);
                                } catch (Exception e) {
                                    log.warn("Directory {} does not exist or is not accessible: {}", testPath, e.getMessage());
                                }
                                
                                operations.disconnect();
                            } catch (Exception e) {
                                log.error("Failed to connect to SFTP server: {}", e.getMessage());
                                throw e;
                            }
                        });
                }
            });
        } catch (Exception e) {
            log.error("Failed to create connection test route: {}", e.getMessage());
            throw new RuntimeException("Failed to test SFTP connection", e);
        }
    }

    private void logFileProcessing(Exchange exchange, String clientName) {
        String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
        Long fileSize = exchange.getIn().getHeader(Exchange.FILE_LENGTH, Long.class);
        String lastModified = exchange.getIn().getHeader(Exchange.FILE_LAST_MODIFIED, String.class);
        String absolutePath = exchange.getIn().getHeader(Exchange.FILE_PATH, String.class);
        String relativePath = exchange.getIn().getHeader("CamelFileRelativePath", String.class);
        
        log.info("=== File Processing Details ===");
        log.info("Client: {}", clientName);
        log.info("File Name: {}", filename);
        log.info("File Size: {} bytes", fileSize);
        log.info("Last Modified: {}", lastModified);
        log.info("Absolute Path: {}", absolutePath);
        log.info("Relative Path: {}", relativePath);
        log.info("============================");
    }

    @SuppressWarnings("unchecked")
    private void buildFileProcessingRoute(String routeId, String uri, SftpConfig config, String clientName) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(uri)
                        .routeId(routeId)
                        .process(exchange -> {
                            log.info("SFTP Consumer - Starting connection to {}:{}", config.getHost(), config.getPort());
                            RemoteFileEndpoint<?> endpoint = (RemoteFileEndpoint<?>) exchange.getFromEndpoint();
                            log.debug("SFTP Consumer - Using endpoint: {}", endpoint.getConfiguration());
                        })
                        .process(exchange -> logFileProcessing(exchange, clientName))
                        .setHeader("ClientId", constant(config.getClient().getId()))
                        .setHeader("InterfaceId", constant(config.getInterfaceId()))
                        .setHeader("filename", header(Exchange.FILE_NAME))
                        .setHeader("CamelSftpUsername", constant(config.getUsername()))
                        .setHeader("CamelSftpHost", constant(config.getHost()))
                        .setHeader("CamelSftpPort", constant(config.getPort()))
                        .setHeader("CamelSftpPassword", constant(config.getPassword()))
                        .setHeader("CamelSftpDirectory", constant("sftp_root/inbound"))
                        .to("direct:processInboundFile")
                        .process(exchange -> {
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            log.info("Successfully sent file to processor: {}", filename);
                        })
                        .toD("sftp://${header.CamelSftpUsername}@${header.CamelSftpHost}:${header.CamelSftpPort}/${header.CamelSftpDirectory}/ok?password=${header.CamelSftpPassword}&fileName=${header.CamelFileName}")
                        .process(exchange -> {
                            // Delete the original file after successful processing
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            String originalPath = "sftp_root/inbound/" + filename;
                            RemoteFileEndpoint<?> endpoint = (RemoteFileEndpoint<?>) exchange.getFromEndpoint();
                            RemoteFileOperations<?> operations = endpoint.createRemoteFileOperations();
                            operations.connect(endpoint.getConfiguration(), exchange);
                            boolean deleted = operations.deleteFile(originalPath);
                            operations.disconnect();
                            log.info("Deleted original file {}: {}", filename, deleted);
                        })
                        .log("Completed processing ${header.CamelFileName} and moved to ok directory");
                }
            });
        } catch (Exception e) {
            handleSftpError(e, "build file processing route", config);
        }
    }

    private String normalizePath(String path, SftpConfig config) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Convert to forward slashes and trim
        String normalizedPath = path.trim().replace("\\", "/");

        // Remove Windows drive letter if present
        if (normalizedPath.matches("^[A-Za-z]:.*")) {
            normalizedPath = normalizedPath.substring(2);
        }

        // Remove leading slashes
        normalizedPath = normalizedPath.replaceAll("^/+", "");

        // Ensure no double slashes and no trailing slash
        normalizedPath = normalizedPath.replaceAll("/+", "/").replaceAll("/$", "");

        log.debug("Path normalization:");
        log.debug("Original path: {}", path);
        log.debug("Normalized path: {}", normalizedPath);

        return normalizedPath;
    }

    private String buildMonitoredPath(SftpConfig config, String monitoredDir) {
        // Return path relative to SFTP user's home directory
        return "sftp_root/inbound";
    }

    private String buildSftpUri(SftpConfig config, String monitoredPath) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(String.format("sftp://%s@%s:%d/%s",
                config.getUsername(),
                config.getHost(),
                config.getPort(),
                monitoredPath));  // Using the relative path

        // Add parameters
        uriBuilder.append("?password=").append(config.getPassword())
                  .append("&initialDelay=1000")
                  .append("&delay=60")
                  .append("&readLock=none")
                  .append("&stepwise=false")
                  .append("&disconnect=true")
                  .append("&maximumReconnectAttempts=3")
                  .append("&reconnectDelay=5000")
                  .append("&connectTimeout=30000")
                  .append("&strictHostKeyChecking=no");

        String knownHostsFile = System.getProperty("user.home") + "/.ssh/known_hosts";
        uriBuilder.append("&knownHostsFile=").append(knownHostsFile);

        return uriBuilder.toString();
    }

    private void ensureDirectoriesExist(SftpConfig config) {
        // No longer creating directories, just logging the paths
        String processedDir = config.getProcessedDirectory();
        if (processedDir == null || processedDir.trim().isEmpty()) {
            processedDir = "ok";
        }
        
        String errorDir = config.getErrorDirectory();
        if (errorDir == null || errorDir.trim().isEmpty()) {
            errorDir = "ko";
        }
        
        log.debug("Using processed directory: {}", processedDir);
        log.debug("Using error directory: {}", errorDir);
    }

    private String buildOkDirectoryPath(SftpConfig config, String filename) {
        return "sftp_root/inbound/ok";
    }

    private String buildKoDirectoryPath(SftpConfig config, String filename) {
        return "sftp_root/inbound/ko";
    }

    private void createOrUpdateRoute(SftpConfig config) {
        try {
            String routeId = buildRouteId(config);
            String clientName = config.getClient() != null ? config.getClient().getName() : "unknown";
            
            log.info("Creating/updating route for client {} with config:", clientName);
            log.info("Remote Directory from DB: {}", config.getRemoteDirectory());
            log.info("Monitored Directories from DB: {}", config.getMonitoredDirectories());
            
            // Process each monitored directory from config
            List<String> monitoredDirs = config.getMonitoredDirectories();
            if (monitoredDirs == null || monitoredDirs.isEmpty()) {
                log.warn("No monitored directories configured for SFTP config: {}", config.getId());
                return;
            }

            // Remove existing routes if any
            if (camelContext.getRoute(routeId) != null) {
                log.debug("Removing existing route with ID: {}", routeId);
                camelContext.removeRoute(routeId);
            }

            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    // Error handling for RabbitMQ connection issues
                    onException(com.rabbitmq.client.AlreadyClosedException.class, com.rabbitmq.client.ShutdownSignalException.class)
                        .maximumRedeliveries(3)
                        .redeliveryDelay(1000)
                        .backOffMultiplier(2)
                        .handled(true)
                        .log("RabbitMQ connection error, will retry: ${exception.message}");

                    // Global error handling
                    onException(Exception.class)
                        .handled(true)
                        .process(exchange -> {
                            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            log.error("=== File Processing Error ===");
                            log.error("Error processing file: {}", filename);
                            log.error("Error details: {}", cause.getMessage());
                            log.info("Moving file to error directory");
                            
                            String koPath = buildKoDirectoryPath(config, filename);
                            String koUri = String.format("sftp://%s@%s:%d/%s?password=%s&stepwise=false&fileName=${file:name}",
                                config.getUsername(),
                                config.getHost(),
                                config.getPort(),
                                koPath,
                                config.getPassword());
                            
                            exchange.setProperty("KoUri", koUri);
                        })
                        .setHeader("DestinationFileName", simple("${date:now:yyyyMMdd-HHmmss}_${file:name}"))
                        .toD("sftp://${header.CamelSftpUsername}@${header.CamelSftpHost}:${header.CamelSftpPort}/${header.CamelSftpDirectory}/ko?password=${header.CamelSftpPassword}&fileName=${header.DestinationFileName}&stepwise=false")
                        .process(exchange -> {
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            log.info("File {} moved to error directory", filename);
                            log.info("=== Error Handling Complete ===");
                        });

                    // Create routes for each monitored directory
                    for (String directory : monitoredDirs) {
                        String monitoredPath = buildMonitoredPath(config, directory);
                        String uri = buildSftpUri(config, monitoredPath);
                        log.debug("Building SFTP URI for directory {}: {}", monitoredPath, uri);
                        
                        from(uri)
                            .routeId(routeId + "-" + directory.replace('/', '-'))
                            .process(exchange -> {
                                log.info("=== SFTP Consumer Starting ===");
                                log.info("Monitoring path: {}", monitoredPath);
                                log.info("SFTP Server: {}:{}", config.getHost(), config.getPort());
                            })
                            .process(exchange -> {
                                String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                                log.info("=== New File Detected: {} ===", filename);
                                logFileProcessing(exchange, clientName);
                            })
                            .setHeader("ClientId", constant(config.getClient().getId()))
                            .setHeader("InterfaceId", constant(config.getInterfaceId()))
                            .setHeader("filename", header(Exchange.FILE_NAME))
                            .setHeader("CamelSftpUsername", constant(config.getUsername()))
                            .setHeader("CamelSftpHost", constant(config.getHost()))
                            .setHeader("CamelSftpPort", constant(config.getPort()))
                            .setHeader("CamelSftpPassword", constant(config.getPassword()))
                            .setHeader("CamelSftpDirectory", constant("sftp_root/inbound"))
                            .process(exchange -> {
                                String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                                log.info("Sending file {} to processing route", filename);
                                
                                String okPath = buildOkDirectoryPath(config, filename);
                                String okUri = String.format("sftp://%s@%s:%d/%s?password=%s&stepwise=false&fileName=${file:name}",
                                    config.getUsername(),
                                    config.getHost(),
                                    config.getPort(),
                                    okPath,
                                    config.getPassword());
                                
                                exchange.setProperty("OkUri", okUri);
                            })
                            // CHANGE: Route to direct:processInboundFile instead of directly to RabbitMQ
                            .to("direct:processInboundFile")
                            .process(exchange -> {
                                String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                                log.info("Processing completed, moving file to processed directory");
                            })
                            .setHeader("DestinationFileName", simple("${date:now:yyyyMMdd-HHmmss}_${file:name}"))
                            .toD("sftp://${header.CamelSftpUsername}@${header.CamelSftpHost}:${header.CamelSftpPort}/${header.CamelSftpDirectory}/ok?password=${header.CamelSftpPassword}&fileName=${header.DestinationFileName}&stepwise=false")
                            .process(exchange -> {
                                // Delete the original file after successful processing
                                String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                                String originalPath = "sftp_root/inbound/" + filename;
                                RemoteFileEndpoint<?> endpoint = (RemoteFileEndpoint<?>) exchange.getFromEndpoint();
                                RemoteFileOperations<?> operations = endpoint.createRemoteFileOperations();
                                operations.connect(endpoint.getConfiguration(), exchange);
                                boolean deleted = operations.deleteFile(originalPath);
                                operations.disconnect();
                                log.info("Deleted original file {}: {}", filename, deleted);
                            })
                            .process(exchange -> {
                                String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                                log.info("=== File Processing Complete ===");
                                log.info("File {} successfully moved to processed directory", filename);
                            });
                    }
                }
            });
            
            log.info("Successfully created/updated SFTP route for client: {} and interface: {}", 
                    clientName, config.getInterfaceId());
        } catch (Exception e) {
            handleSftpError(e, "create/update route", config);
        }
    }

    private String buildRouteId(SftpConfig config) {
        return String.format("sftp-%d-%d", config.getClient().getId(), config.getInterfaceId());
    }

    private void removeInactiveRoutes(List<SftpConfig> activeConfigs) {
        Set<String> activeRouteIds = activeConfigs.stream()
            .map(this::buildRouteId)
            .collect(Collectors.toSet());

        camelContext.getRoutes().stream()
            .filter(route -> route.getId().startsWith("sftp-"))
            .filter(route -> !activeRouteIds.contains(route.getId()))
            .forEach(route -> {
                try {
                    camelContext.removeRoute(route.getId());
                    log.info("Removed inactive route: {}", route.getId());
                } catch (Exception e) {
                    log.error("Failed to remove route: " + route.getId(), e);
                }
            });
    }

    private void logSftpConfigDetails(String clientName, SftpConfig config) {
        log.debug("SFTP Configuration Details for client: {}, interface: {}", 
            clientName, config.getInterfaceId());
        log.debug("Host: {}, Port: {}, Username: {}", 
            config.getHost(), config.getPort(), config.getUsername());
        log.debug("Remote Directory: {}", config.getRemoteDirectory());
        log.debug("Monitored Directories: {}", config.getMonitoredDirectories());
        log.debug("Processed Directory: {}, Error Directory: {}", 
            config.getProcessedDirectory(), config.getErrorDirectory());
    }

    private void initializeInboundRoute() {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    // Global error handling for inbound processing
                    onException(Exception.class)
                        .handled(true)
                        .maximumRedeliveries(3)
                        .redeliveryDelay(1000)
                        .backOffMultiplier(2)
                        .useExponentialBackOff()
                        .process(exchange -> {
                            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            log.error("Error processing inbound file {}: {}", filename, cause.getMessage());
                            exchange.getIn().setHeader("ProcessingStatus", "ERROR");
                            exchange.getIn().setHeader("ErrorMessage", cause.getMessage());
                        })
                        .to("direct:errorHandler");

                    // Main inbound processing route
                    from("direct:processInboundFile")
                        .routeId("inbound-file-processor")
                        .log(LoggingLevel.INFO, "Processing inbound file: ${header.CamelFileName}")
                        
                        // Validate required headers
                        .process(exchange -> {
                            String filename = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                            Long clientId = exchange.getIn().getHeader("ClientId", Long.class);
                            Long interfaceId = exchange.getIn().getHeader("InterfaceId", Long.class);
                            
                            if (filename == null || clientId == null || interfaceId == null) {
                                throw new IllegalArgumentException("Missing required headers: filename, ClientId, or InterfaceId");
                            }
                        })
                        
                        // Set processing headers
                        .setHeader("filename", simple("${header.CamelFileName}"))
                        .setHeader("ProcessingStatus", constant("PROCESSING"))
                        
                        // CHANGE: Updated RabbitMQ configuration to prevent auto-generated queues
                        .to("rabbitmq:middleware.direct?routingKey=inbound.processor" +
                            "&addresses={{spring.rabbitmq.host:localhost}}:{{spring.rabbitmq.port:5672}}" +
                            "&username={{spring.rabbitmq.username:admin}}" +
                            "&password={{spring.rabbitmq.password:admin}}" +
                            "&mandatory=true" +
                            "&autoDelete=false" +
                            "&durable=true" +
                            "&exchangeType=direct" +
                            "&declare=false" +           // Prevent exchange declaration
                            "&skipQueueDeclare=true" +   // Skip queue declaration
                            "&transferException=true" +   // Better error handling
                            "&automaticRecoveryEnabled=true" +
                            "&requestedHeartbeat=60" +
                            "&networkRecoveryInterval=5000" +
                            "&connectionTimeout=10000" +
                            "&requestedChannelMax=5" +
                            "&publisherAcknowledgements=true")
                        
                        .log(LoggingLevel.INFO, "File sent to processor queue: ${header.CamelFileName}")
                        
                        // Handle processing result
                        .choice()
                            .when(header("ProcessingStatus").isEqualTo("ERROR"))
                                .log(LoggingLevel.ERROR, "Processing failed for file: ${header.CamelFileName}")
                                .to("direct:errorHandler")
                            .otherwise()
                                .log(LoggingLevel.INFO, "Processing completed for file: ${header.CamelFileName}")
                        .end();

                    // Error handling route
                    from("direct:errorHandler")
                        .routeId("error-handler")
                        .log(LoggingLevel.ERROR, "Error processing file: ${header.CamelFileName}")
                        .log(LoggingLevel.ERROR, "Error details: ${header.ErrorMessage}")
                        .process(exchange -> {
                            // Here you can add additional error handling logic
                            // For example, sending notifications, updating error counts, etc.
                        });
                }
            });
            
            log.info("Successfully initialized inbound file processing route");
        } catch (Exception e) {
            log.error("Failed to initialize inbound file processing route: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize inbound file processing route", e);
        }
    }
}