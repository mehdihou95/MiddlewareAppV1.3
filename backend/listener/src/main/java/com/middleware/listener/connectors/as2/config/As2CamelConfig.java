package com.middleware.listener.connectors.as2.config;

import com.middleware.shared.model.connectors.As2Config;
import com.middleware.listener.connectors.as2.service.As2ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.internal.AS2ApiName;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class As2CamelConfig {
    private final As2ConfigService as2ConfigService;
    private final CamelContext camelContext;
    private final As2Properties as2Properties;

    @PostConstruct
    public void initializeRoutes() {
        updateRoutes();
    }

    @Scheduled(fixedDelay = 60000) // Check for config changes every minute
    public void updateRoutes() {
        List<As2Config> activeConfigs = as2ConfigService.getActiveConfigurations();
        
        // Remove inactive routes
        removeInactiveRoutes(activeConfigs);
        
        // Add or update routes for active configs
        for (As2Config config : activeConfigs) {
            createOrUpdateRoute(config);
        }
    }

    private void createOrUpdateRoute(As2Config config) {
        try {
            String routeId = buildRouteId(config);
            
            // Remove existing route if it exists
            if (camelContext.getRoute(routeId) != null) {
                camelContext.removeRoute(routeId);
            }

            // Create new route
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    // Error handling
                    errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(3)
                        .redeliveryDelay(5000)
                        .backOffMultiplier(2)
                        .useExponentialBackOff());

                    // AS2 Server endpoint to receive messages
                    from(buildAs2Uri(config))
                        .routeId(routeId)
                        .log("Received AS2 message: ${header.AS2MessageId} for client: " + config.getClient().getName())
                        .setHeader("ClientId", constant(config.getClient().getId()))
                        .setHeader("InterfaceId", constant(config.getInterfaceConfig().getId()))
                        .choice()
                            .when(header("AS2-MDN-Required").isEqualTo(true))
                                .to("direct:generateMDN")
                            .end()
                        .to("direct:processAs2Message")
                        .log("AS2 message processed: ${header.AS2MessageId}");
                }
            });
            
            log.info("Created/Updated AS2 route for client: {} and interface: {}", 
                    config.getClient().getId(), config.getInterfaceConfig().getId());
        } catch (Exception e) {
            log.error("Failed to create/update route for config: " + config.getId(), e);
        }
    }

    private String buildRouteId(As2Config config) {
        return String.format("as2-%d-%d", config.getClient().getId(), config.getInterfaceConfig().getId());
    }

    private String buildAs2Uri(As2Config config) {
        StringBuilder uri = new StringBuilder();
        uri.append("as2://server")
           .append("?apiName=").append(config.getApiName())
           .append("&partnerId=").append(config.getPartnerId())
           .append("&localId=").append(config.getLocalId())
           .append("&encryptionAlgorithm=").append(config.getEncryptionAlgorithm())
           .append("&signatureAlgorithm=").append(config.getSignatureAlgorithm())
           .append("&compression=").append(config.isCompression())
           .append("&mdnMode=").append(config.getMdnMode())
           .append("&mdnDigestAlgorithm=").append(config.getMdnDigestAlgorithm())
           .append("&encryptMessage=").append(config.isEncryptMessage())
           .append("&signMessage=").append(config.isSignMessage())
           .append("&requestMdn=").append(config.isRequestMdn());

        if (config.getMdnUrl() != null && !config.getMdnUrl().isEmpty()) {
            uri.append("&mdnUrl=").append(config.getMdnUrl());
        }

        return uri.toString();
    }

    private void removeInactiveRoutes(List<As2Config> activeConfigs) {
        Set<String> activeRouteIds = activeConfigs.stream()
                .map(this::buildRouteId)
                .collect(Collectors.toSet());

        camelContext.getRoutes().stream()
                .filter(route -> route.getId().startsWith("as2-"))
                .filter(route -> !activeRouteIds.contains(route.getId()))
                .forEach(route -> {
                    try {
                        camelContext.removeRoute(route.getId());
                        log.info("Removed inactive AS2 route: {}", route.getId());
                    } catch (Exception e) {
                        log.error("Failed to remove route: " + route.getId(), e);
                    }
                });
    }

    @Bean
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        keyStoreParameters.setResource(as2Properties.getKeyStorePath());
        keyStoreParameters.setPassword(as2Properties.getKeyStorePassword());

        KeyManagersParameters keyManagersParameters = new KeyManagersParameters();
        keyManagersParameters.setKeyStore(keyStoreParameters);
        keyManagersParameters.setKeyPassword(as2Properties.getPrivateKeyPassword());

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(keyManagersParameters);
        return sslContextParameters;
    }

    @Bean
    public AS2Component as2Component() {
        AS2Component as2Component = new AS2Component();
        AS2Configuration config = new AS2Configuration();
        config.setApiName(AS2ApiName.SERVER);
        as2Component.setConfiguration(config);
        return as2Component;
    }
} 