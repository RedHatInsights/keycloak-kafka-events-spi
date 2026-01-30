package com.redhat.keycloak.kafka.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;

/**
 * Enhanced Kafka producer factory that integrates with SecurityConfiguration
 * for comprehensive security support including SASL authentication and
 * advanced certificate handling.
 */
public final class KafkaProducerFactory implements KafkaProducerInterface {

    private static final Logger LOG = Logger.getLogger(KafkaProducerFactory.class);

    @Override
    public Producer<String, String> createProducer(String clientId, String bootstrapServer,
                                                   Map<String, Object> optionalProperties) {
        return createProducer(clientId, bootstrapServer, optionalProperties, new HashMap<>());
    }

    /**
     * Creates a Kafka producer with enhanced security configuration.
     *
     * @param clientId Kafka client ID
     * @param bootstrapServer Bootstrap server(s)
     * @param optionalProperties Additional producer properties
     * @param environmentVariables Environment variables for security configuration
     * @return Configured Kafka producer
     */
    public Producer<String, String> createProducer(String clientId, String bootstrapServer,
                                                   Map<String, Object> optionalProperties,
                                                   Map<String, String> environmentVariables) {
        LOG.debug("Creating enhanced Kafka producer with security configuration");

        Properties props = new Properties();

        // Set basic producer properties
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Add optional properties
        props.putAll(optionalProperties);

        // Add enhanced security properties
        if (!environmentVariables.isEmpty()) {
            try {
                SecurityConfiguration securityConfig = new SecurityConfiguration(environmentVariables);
                securityConfig.validateConfiguration();

                Map<String, Object> securityProperties = securityConfig.getSecurityProperties();
                props.putAll(securityProperties);

                LOG.info("Enhanced security configuration applied to Kafka producer");

                // Log security protocol without exposing sensitive details
                String securityProtocol = (String) securityProperties.get("security.protocol");
                if (securityProtocol != null) {
                    LOG.info("Kafka producer security protocol: " + securityProtocol);
                }

                String saslMechanism = (String) securityProperties.get("sasl.mechanism");
                if (saslMechanism != null) {
                    LOG.info("Kafka producer SASL mechanism: " + saslMechanism);
                }

            } catch (SecurityConfiguration.SecurityConfigurationException e) {
                LOG.error("Failed to apply security configuration", e);
                throw new RuntimeException("Kafka producer security configuration failed: " + e.getMessage(), e);
            }
        } else {
            LOG.debug("No security environment variables provided, using basic configuration");
        }

        // Create and return the producer
        try {
            Producer<String, String> producer = new KafkaProducer<>(props);
            LOG.info("Kafka producer created successfully");
            return producer;
        } catch (Exception e) {
            LOG.error("Failed to create Kafka producer", e);
            throw new RuntimeException("Failed to create Kafka producer: " + e.getMessage(), e);
        }
    }
}
