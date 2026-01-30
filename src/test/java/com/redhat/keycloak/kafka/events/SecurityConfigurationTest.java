package com.redhat.keycloak.kafka.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive unit tests for SecurityConfiguration class.
 * Tests CA certificate handling, SASL configuration, and error scenarios.
 */
class SecurityConfigurationTest {

    private Map<String, String> envVars;

    @BeforeEach
    void setUp() {
        envVars = new HashMap<>();
    }

    @Test
    @DisplayName("Should initialize with empty environment variables")
    void shouldInitializeWithEmptyEnvironmentVariables() {
        SecurityConfiguration config = new SecurityConfiguration(envVars);

        Map<String, Object> properties = config.getSecurityProperties();
        assertTrue(properties.isEmpty(), "Security properties should be empty when no environment variables are provided");

        assertDoesNotThrow(() -> config.validateConfiguration());
    }

    @Test
    @DisplayName("Should configure SASL_SSL with PLAIN mechanism")
    void shouldConfigureSASL_SSLWithPLAINMechanism() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "PLAIN");
        envVars.put("KAFKA_SASL_USERNAME", "testuser");
        envVars.put("KAFKA_SASL_PASSWORD", "testpass");

        SecurityConfiguration config = new SecurityConfiguration(envVars);
        Map<String, Object> properties = config.getSecurityProperties();

        assertEquals("SASL_SSL", properties.get("security.protocol"));
        assertEquals("PLAIN", properties.get("sasl.mechanism"));
        assertNotNull(properties.get("sasl.jaas.config"));
        assertTrue(properties.get("sasl.jaas.config").toString().contains("testuser"));
        // Note: passwords are included in JAAS config as required by Kafka, but should be handled securely in production
        assertTrue(properties.get("sasl.jaas.config").toString().contains("testpass"), "Password should be present in JAAS config for Kafka");

        assertDoesNotThrow(() -> config.validateConfiguration());
    }

    @Test
    @DisplayName("Should configure SASL_SSL with SCRAM-SHA-256 mechanism")
    void shouldConfigureSASL_SSLWithSCRAMMechanism() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "SCRAM-SHA-256");
        envVars.put("KAFKA_SASL_SCRAM_USERNAME", "testuser");
        envVars.put("KAFKA_SASL_SCRAM_PASSWORD", "testpass");

        SecurityConfiguration config = new SecurityConfiguration(envVars);
        Map<String, Object> properties = config.getSecurityProperties();

        assertEquals("SASL_SSL", properties.get("security.protocol"));
        assertEquals("SCRAM-SHA-256", properties.get("sasl.mechanism"));
        assertNotNull(properties.get("sasl.jaas.config"));
        assertTrue(properties.get("sasl.jaas.config").toString().contains("ScramLoginModule"));
    }

    @Test
    @DisplayName("Should configure SASL_SSL with GSSAPI mechanism")
    void shouldConfigureSASL_SSLWithGSSAPIMechanism() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "GSSAPI");
        envVars.put("KAFKA_SASL_KERBEROS_SERVICE_NAME", "kafka");
        envVars.put("KAFKA_SASL_KERBEROS_PRINCIPAL", "kafka-client@EXAMPLE.COM");

        SecurityConfiguration config = new SecurityConfiguration(envVars);
        Map<String, Object> properties = config.getSecurityProperties();

        assertEquals("SASL_SSL", properties.get("security.protocol"));
        assertEquals("GSSAPI", properties.get("sasl.mechanism"));
        assertEquals("kafka", properties.get("sasl.kerberos.service.name"));
        assertTrue(properties.get("sasl.jaas.config").toString().contains("Krb5LoginModule"));
    }

    @Test
    @DisplayName("Should configure SASL_SSL with OAUTHBEARER mechanism")
    void shouldConfigureSASL_SSLWithOAuthBearerMechanism() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "OAUTHBEARER");
        envVars.put("KAFKA_SASL_OAUTH_TOKEN", "test-token");
        envVars.put("KAFKA_SASL_OAUTH_TOKEN_ENDPOINT", "https://auth.example.com/token");

        SecurityConfiguration config = new SecurityConfiguration(envVars);
        Map<String, Object> properties = config.getSecurityProperties();

        assertEquals("SASL_SSL", properties.get("security.protocol"));
        assertEquals("OAUTHBEARER", properties.get("sasl.mechanism"));
        assertTrue(properties.get("sasl.jaas.config").toString().contains("OAuthBearerLoginModule"));
    }

    @Test
    @DisplayName("Should fail validation with invalid security protocol")
    void shouldFailValidationWithInvalidSecurityProtocol() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "INVALID_PROTOCOL");

        SecurityConfiguration config = new SecurityConfiguration(envVars);

        SecurityConfiguration.SecurityConfigurationException exception = assertThrows(
            SecurityConfiguration.SecurityConfigurationException.class,
            () -> config.validateConfiguration()
        );
        assertTrue(exception.getMessage().contains("Invalid security protocol"));
    }

    @Test
    @DisplayName("Should fail with invalid CA certificate format")
    void shouldFailWithInvalidCACertificateFormat() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SSL");
        envVars.put("KAFKA_SSL_CA_CERTIFICATE", "invalid-certificate-format");

        SecurityConfiguration.SecurityConfigurationException exception = assertThrows(
            SecurityConfiguration.SecurityConfigurationException.class,
            () -> new SecurityConfiguration(envVars)
        );
        assertTrue(exception.getMessage().contains("Security configuration failed: Invalid CA certificate format"));
    }

    @Test
    @DisplayName("Should fail with missing SASL username for PLAIN mechanism")
    void shouldFailWithMissingSASLUsername() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "PLAIN");
        envVars.put("KAFKA_SASL_PASSWORD", "password");
        // Missing username

        assertThrows(
            SecurityConfiguration.SecurityConfigurationException.class,
            () -> new SecurityConfiguration(envVars)
        );
    }

    @Test
    @DisplayName("Should fail with missing SASL password for PLAIN mechanism")
    void shouldFailWithMissingSASLPassword() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "PLAIN");
        envVars.put("KAFKA_SASL_USERNAME", "username");
        // Missing password

        assertThrows(
            SecurityConfiguration.SecurityConfigurationException.class,
            () -> new SecurityConfiguration(envVars)
        );
    }

    @Test
    @DisplayName("Should handle multiple SASL properties")
    void shouldHandleMultipleSASLProperties() {
        envVars.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        envVars.put("KAFKA_SASL_MECHANISM", "GSSAPI");
        envVars.put("KAFKA_SASL_KERBEROS_SERVICE_NAME", "kafka");
        envVars.put("KAFKA_SASL_KERBEROS_PRINCIPAL", "client@REALM");
        envVars.put("KAFKA_SASL_LOGIN_REFRESH_BUFFER_SECONDS", "60");
        envVars.put("KAFKA_SASL_KERBEROS_KINIT_CMD", "/usr/bin/kinit");

        SecurityConfiguration config = new SecurityConfiguration(envVars);
        Map<String, Object> properties = config.getSecurityProperties();

        assertEquals("kafka", properties.get("sasl.kerberos.service.name"));
        assertEquals("60", properties.get("sasl.login.refresh.buffer.seconds"));
        assertEquals("/usr/bin/kinit", properties.get("sasl.kerberos.kinit.cmd"));
    }
}
