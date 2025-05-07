package com.middleware.listener.connectors.api.config;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiCamelConfig {

    private final ApiProperties apiProperties;

    public ApiCamelConfig(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    @Bean
    public RouteBuilder apiRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Error handling
                onException(Exception.class)
                    .maximumRedeliveries(apiProperties.getMaxRetries())
                    .redeliveryDelay(apiProperties.getRetryDelay())
                    .backOffMultiplier(2)
                    .useExponentialBackOff()
                    .handled(true)
                    .to("file:" + apiProperties.getErrorDirectory());

                // REST configuration
                restConfiguration()
                    .component("servlet")
                    .bindingMode(RestBindingMode.json)
                    .dataFormatProperty("prettyPrint", "true")
                    .contextPath(apiProperties.getContextPath())
                    .port(apiProperties.getPort())
                    .enableCORS(true)
                    .corsAllowCredentials(true)
                    .corsHeaderProperty("Access-Control-Allow-Origin", String.join(",", apiProperties.getAllowedOrigins()))
                    .apiProperty("api.title", "Middleware API")
                    .apiProperty("api.version", "1.0.0");

                // Rate limiting
                if (apiProperties.isEnableRateLimit()) {
                    from("rest:get:/messages/*")
                        .routeId("rate-limiter")
                        .throttle(apiProperties.getRateLimit())
                        .timePeriodMillis(apiProperties.getRateLimitPeriodSeconds() * 1000L);
                }

                // REST endpoints
                rest("/messages")
                    .post()
                        .consumes("application/json")
                        .produces("application/json")
                        .to("direct:processMessage")
                    .get("/{id}")
                        .produces("application/json")
                        .to("direct:getMessage");

                // Message validation route
                from("direct:validateMessage")
                    .routeId("validate-message")
                    .log("Validating message")
                    .choice()
                        .when(simple("${body} == null"))
                            .throwException(new IllegalArgumentException("Message body cannot be null"))
                        .end();

                // Message processing route
                from("direct:processMessage")
                    .routeId("api-message-processor")
                    .log("Processing message")
                    .choice()
                        .when(simple("${exception} == null"))
                            .to("file:" + apiProperties.getProcessedDirectory())
                            .to("rabbitmq:middleware.direct?routingKey=inbound.processor")
                        .otherwise()
                            .to("file:" + apiProperties.getErrorDirectory())
                    .end();

                // Message retrieval route
                from("direct:getMessage")
                    .routeId("get-message-route")
                    .log("Retrieving message details")
                    .setBody(simple("{\"id\":\"${header.id}\",\"status\":\"processed\"}"));
            }
        };
    }
} 