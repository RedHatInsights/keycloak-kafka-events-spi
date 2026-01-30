package com.redhat.keycloak.kafka.events;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * Central security configuration class that handles SSL certificate configuration
 * using KAFKA_SSL_CA_CERTIFICATE and SASL authentication setup for Kafka producers.
 */
public class SecurityConfiguration {

    private static final Logger LOG = Logger.getLogger(SecurityConfiguration.class);
    private static final String TEMP_DIR = "/tmp";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    private final Map<String, String> environmentVariables;
    private final Map<String, Object> securityProperties;

    public SecurityConfiguration(Map<String, String> environmentVariables) {
        this.environmentVariables = new HashMap<>(environmentVariables);
        this.securityProperties = new HashMap<>();
        initializeSecurityConfiguration();
    }

    /**
     * Initializes the complete security configuration by processing
     * environment variables and setting up certificates and SASL.
     */
    private void initializeSecurityConfiguration() {
        try {
            // Process security protocol first
            configureSecurityProtocol();

            // Process CA certificate configuration
            configureCACertificate();

            // Process SASL authentication if required
            configureSASLAuthentication();

            LOG.info("Security configuration initialized successfully");
        } catch (Exception e) {
            LOG.error("DEBUG: Exception caught in initializeSecurityConfiguration: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            LOG.error("Failed to initialize security configuration", e);
            throw new SecurityConfigurationException("Security configuration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Configures the security protocol based on environment variables.
     */
    private void configureSecurityProtocol() {
        String securityProtocol = getEnvVar("KAFKA_SECURITY_PROTOCOL");
        if (securityProtocol != null) {
            securityProperties.put("security.protocol", securityProtocol);
        }
    }

    /**
     * Configures SSL truststore from KAFKA_SSL_CA_CERTIFICATE.
     */
    private void configureCACertificate() throws CertificateException {
        String caCertificate = getEnvVar("KAFKA_SSL_CA_CERTIFICATE");
        if (caCertificate != null && !caCertificate.trim().isEmpty()) {
            validatePEMCertificateFormat(caCertificate, "CA certificate");

            try {
                KeyStore truststore = createTruststore(caCertificate);
                String truststoreFile = createKeystoreFile(truststore);
                securityProperties.put("ssl.truststore.location", truststoreFile);
                securityProperties.put("ssl.truststore.password", getTruststorePassword());
                securityProperties.put("ssl.truststore.type", getKeystoreType());

                LOG.info("CA certificate configured successfully");
            } catch (Exception e) {
                LOG.error("Failed to configure CA certificate", e);
                throw new CertificateException("CA certificate configuration failed: " + e.getMessage(), e);
            }
        } else {
            LOG.debug("No CA certificate provided");
        }

        // Add additional SSL configuration if provided
        addAdditionalSSLProperties();
    }

    /**
     * Creates a truststore containing the CA certificate.
     */
    private KeyStore createTruststore(String caCertificate) throws Exception {
        KeyStore truststore = KeyStore.getInstance(getKeystoreType());
        truststore.load(null, null);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate caCert = certFactory.generateCertificate(
            new ByteArrayInputStream(caCertificate.getBytes()));

        truststore.setCertificateEntry("kafka-ca", caCert);
        return truststore;
    }

    /**
     * Creates a temporary keystore file from the provided KeyStore and returns its path.
     */
    private String createKeystoreFile(KeyStore keystore) throws Exception {
        Path tempFile = Files.createTempFile(Paths.get(TEMP_DIR), "kafka-truststore", ".jks");

        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            keystore.store(fos, getTruststorePassword().toCharArray());
        }

        LOG.debug("Created temporary truststore file: " + tempFile.toAbsolutePath());
        return tempFile.toAbsolutePath().toString();
    }

    /**
     * Validates PEM certificate format.
     */
    private void validatePEMCertificateFormat(String certificate, String description) throws CertificateException {
        if (certificate == null || certificate.trim().isEmpty()) {
            throw new CertificateException(description + " cannot be null or empty");
        }

        if (!certificate.contains("-----BEGIN CERTIFICATE-----") ||
            !certificate.contains("-----END CERTIFICATE-----")) {
            throw new CertificateException("Invalid " + description + " format: " +
                "Missing PEM certificate boundaries");
        }

        String[] lines = certificate.split("\n");
        if (lines.length < 3) {
            throw new CertificateException("Invalid " + description + " format: " +
                "Certificate appears to be incomplete");
        }
    }

    /**
     * Adds additional SSL configuration properties from environment variables.
     */
    private void addAdditionalSSLProperties() {
        String sslProtocols = getEnvVar("KAFKA_SSL_ENABLED_PROTOCOLS");
        if (sslProtocols != null && !sslProtocols.trim().isEmpty()) {
            securityProperties.put("ssl.enabled.protocols", sslProtocols.trim());
        }

        String cipherSuites = getEnvVar("KAFKA_SSL_CIPHER_SUITES");
        if (cipherSuites != null && !cipherSuites.trim().isEmpty()) {
            securityProperties.put("ssl.cipher.suites", cipherSuites.trim());
        }

        // Set endpoint identification algorithm - use empty string to disable hostname verification
        if (environmentVariables.containsKey("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM")) {
            String endpointIdentification = getEnvVar("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM");
            securityProperties.put("ssl.endpoint.identification.algorithm",
                endpointIdentification != null ? endpointIdentification.trim() : "");
        }
    }

    /**
     * Configures SASL authentication based on the security protocol and mechanism.
     */
    private void configureSASLAuthentication() {
        String securityProtocol = (String) securityProperties.get("security.protocol");
        if (securityProtocol == null || !securityProtocol.startsWith("SASL")) {
            LOG.debug("SASL not required for security protocol: " + securityProtocol);
            return;
        }

        SASLConfigurationBuilder saslBuilder = new SASLConfigurationBuilder();
        Map<String, Object> saslProperties = saslBuilder.buildSASLConfiguration(environmentVariables);
        securityProperties.putAll(saslProperties);

        LOG.debug("SASL authentication configured");
    }

    /**
     * Gets the keystore type from environment variables or defaults to JKS.
     */
    private String getKeystoreType() {
        String keystoreType = getEnvVar("KAFKA_SSL_KEYSTORE_TYPE_DYNAMIC");
        if (keystoreType != null && !keystoreType.trim().isEmpty()) {
            String type = keystoreType.trim().toUpperCase();
            if (type.equals("JKS") || type.equals("PKCS12")) {
                return type;
            }
            LOG.warn("Invalid keystore type: " + type + ", using default JKS");
        }
        return DEFAULT_KEYSTORE_TYPE;
    }

    /**
     * Gets the truststore password from environment variables or generates a default.
     */
    private String getTruststorePassword() {
        String password = getEnvVar("KAFKA_SSL_TRUSTSTORE_PASSWORD_DYNAMIC");
        if (password != null && !password.trim().isEmpty()) {
            return password.trim();
        }
        return DEFAULT_KEYSTORE_PASSWORD;
    }

    /**
     * Gets an environment variable by name.
     */
    private String getEnvVar(String name) {
        return environmentVariables.get(name);
    }

    /**
     * Returns the complete security properties map for Kafka producer configuration.
     */
    public Map<String, Object> getSecurityProperties() {
        return new HashMap<>(securityProperties);
    }

    /**
     * Validates the current security configuration.
     */
    public void validateConfiguration() throws SecurityConfigurationException {
        if (securityProperties.isEmpty()) {
            LOG.debug("No security configuration to validate");
            return;
        }

        String securityProtocol = (String) securityProperties.get("security.protocol");
        if (securityProtocol != null) {
            validateSecurityProtocol(securityProtocol);
        }

        LOG.debug("Security configuration validation completed");
    }

    /**
     * Validates the security protocol configuration.
     */
    private void validateSecurityProtocol(String securityProtocol) throws SecurityConfigurationException {
        boolean isValid = securityProtocol.equals("PLAINTEXT") ||
                         securityProtocol.equals("SSL") ||
                         securityProtocol.equals("SASL_PLAINTEXT") ||
                         securityProtocol.equals("SASL_SSL");

        if (!isValid) {
            throw new SecurityConfigurationException("Invalid security protocol: " + securityProtocol);
        }
    }

    /**
     * Exception thrown when security configuration fails.
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }

        public SecurityConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
