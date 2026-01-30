package com.redhat.keycloak.kafka.events;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Enhanced factory that initializes security configuration for the Kafka event listener.
 * Supports comprehensive SASL authentication and certificate handling.
 */
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(KafkaEventListenerProviderFactory.class);
    private static final String ID = "kafka";

    private KafkaEventListenerProvider instance;

    private String bootstrapServers;
    private String topicEvents;
    private String topicAdminEvents;
    private String clientId;
    private String[] events;
    private Map<String, Object> kafkaProducerProperties;
    private Map<String, String> environmentVariables;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        if (instance == null) {
            KafkaProducerFactory factory = new KafkaProducerFactory();
            instance = new KafkaEventListenerProvider(bootstrapServers, clientId, topicEvents, events, topicAdminEvents,
                kafkaProducerProperties, environmentVariables, factory);
        }

        return instance;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Scope config) {
        LOG.info("Init enhanced kafka module with security support ...");

        // Initialize basic configuration
        clientId = config.get("clientId", System.getenv("KAFKA_CLIENT_ID"));
        bootstrapServers = config.get("bootstrapServers", System.getenv("KAFKA_BOOTSTRAP_HOST"));
        topicEvents = config.get("topicEvents", System.getenv("KAFKA_TOPIC"));
        topicAdminEvents = config.get("topicAdminEvents", System.getenv("KAFKA_ADMIN_TOPIC"));

        String eventsString = config.get("events", System.getenv("KAFKA_EVENTS"));

        if (eventsString != null) {
            events = eventsString.split(",");
        }

        // Validate required configuration
        if (topicEvents == null) {
            throw new NullPointerException("topic must not be null.");
        }

        if (clientId == null) {
            throw new NullPointerException("clientId must not be null.");
        }

        if (bootstrapServers == null) {
            throw new NullPointerException("bootstrapServers must not be null");
        }

        if (events == null || events.length == 0) {
            events = new String[1];
            events[0] = "REGISTER";
        }

        // Initialize producer properties
        kafkaProducerProperties = KafkaProducerConfig.init(config);

        // Collect all environment variables for security configuration
        environmentVariables = collectEnvironmentVariables();

        // Validate security configuration if provided
        if (!environmentVariables.isEmpty()) {
            try {
                SecurityConfiguration securityConfig = new SecurityConfiguration(environmentVariables);
                securityConfig.validateConfiguration();
                LOG.info("Security validation completed successfully");

                // Log non-sensitive security configuration
                String securityProtocol = environmentVariables.get("KAFKA_SECURITY_PROTOCOL");
                if (securityProtocol != null) {
                    LOG.info("Security protocol configured: " + securityProtocol);
                }

                String saslMechanism = environmentVariables.get("KAFKA_SASL_MECHANISM");
                if (saslMechanism != null) {
                    LOG.info("SASL mechanism configured: " + saslMechanism);
                }

            } catch (SecurityConfiguration.SecurityConfigurationException e) {
                LOG.error("Security configuration validation failed during initialization", e);
                throw new RuntimeException("Failed to initialize Kafka security configuration: " + e.getMessage(), e);
            }
        } else {
            LOG.info("No security environment variables detected, using basic configuration");
        }

        LOG.info("Enhanced Kafka module initialization completed");
    }

    /**
     * Collects all relevant environment variables for security configuration.
     */
    private Map<String, String> collectEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();

        // Collect all environment variables starting with KAFKA_
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("KAFKA_") && value != null && !value.trim().isEmpty()) {
                envVars.put(key, value);
            }
        });

        LOG.debug("Collected " + envVars.size() + " Kafka environment variables for security configuration");
        return envVars;
    }

    @Override
    public void postInit(KeycloakSessionFactory arg0) {
        // ignore
    }

    @Override
    public void close() {
        // ignore
    }
}
