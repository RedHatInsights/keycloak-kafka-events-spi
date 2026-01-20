package com.redhat.keycloak.kafka.events;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * Builds SASL authentication configurations for Kafka producer.
 * Supports PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, GSSAPI, and OAUTHBEARER mechanisms.
 */
public class SASLConfigurationBuilder {

    private static final Logger LOG = Logger.getLogger(SASLConfigurationBuilder.class);

    /**
     * Builds SASL configuration based on environment variables.
     *
     * @param envVars Map of environment variables containing SASL configuration
     * @return Map of SASL properties for Kafka producer configuration
     */
    public Map<String, Object> buildSASLConfiguration(Map<String, String> envVars) {
        LOG.debug("Building SASL configuration");

        Map<String, Object> saslProperties = new HashMap<>();

        try {
            // Get SASL mechanism
            String saslMechanism = envVars.get("KAFKA_SASL_MECHANISM");
            if (saslMechanism == null || saslMechanism.trim().isEmpty()) {
                LOG.warn("SASL mechanism not specified, skipping SASL configuration");
                return saslProperties;
            }

            saslMechanism = saslMechanism.trim().toUpperCase();
            saslProperties.put("sasl.mechanism", saslMechanism);

            // Build JAAS configuration based on mechanism
            String jaasConfig = buildJAASConfiguration(saslMechanism, envVars);
            if (jaasConfig != null) {
                saslProperties.put("sasl.jaas.config", jaasConfig);
            }

            // Add mechanism-specific properties
            addMechanismSpecificProperties(saslProperties, saslMechanism, envVars);

            LOG.info("SASL configuration built successfully for mechanism: " + saslMechanism);

        } catch (Exception e) {
            LOG.error("Failed to build SASL configuration", e);
            throw new SecurityConfigurationException("SASL configuration failed: " + e.getMessage(), e);
        }

        return saslProperties;
    }

    /**
     * Builds JAAS configuration string based on the SASL mechanism.
     */
    private String buildJAASConfiguration(String mechanism, Map<String, String> envVars) {
        switch (mechanism) {
            case "PLAIN":
                return buildPlainJAASConfig(envVars);
            case "SCRAM-SHA-256":
            case "SCRAM-SHA-512":
                return buildScramJAASConfig(envVars);
            case "GSSAPI":
                return buildGSSAPIJAASConfig(envVars);
            case "OAUTHBEARER":
                return buildOAuthBearerJAASConfig(envVars);
            default:
                throw new SecurityConfigurationException("Unsupported SASL mechanism: " + mechanism);
        }
    }

    /**
     * Builds JAAS configuration for PLAIN mechanism.
     */
    private String buildPlainJAASConfig(Map<String, String> envVars) {
        String username = envVars.get("KAFKA_SASL_USERNAME");
        String password = envVars.get("KAFKA_SASL_PASSWORD");

        if (username == null || username.trim().isEmpty()) {
            throw new SecurityConfigurationException("Username is required for PLAIN SASL mechanism");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new SecurityConfigurationException("Password is required for PLAIN SASL mechanism");
        }

        return "org.apache.kafka.common.security.plain.PlainLoginModule required \n" +
               "  username=\"" + username.trim() + "\" \n" +
               "  password=\"" + password.trim() + "\";";
    }

    /**
     * Builds JAAS configuration for SCRAM mechanisms.
     */
    private String buildScramJAASConfig(Map<String, String> envVars) {
        String username = envVars.get("KAFKA_SASL_SCRAM_USERNAME");
        String password = envVars.get("KAFKA_SASL_SCRAM_PASSWORD");

        // Fallback to generic username/password if SCRAM-specific ones are not provided
        if (username == null || username.trim().isEmpty()) {
            username = envVars.get("KAFKA_SASL_USERNAME");
        }
        if (password == null || password.trim().isEmpty()) {
            password = envVars.get("KAFKA_SASL_PASSWORD");
        }

        if (username == null || username.trim().isEmpty()) {
            throw new SecurityConfigurationException("Username is required for SCRAM SASL mechanism");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new SecurityConfigurationException("Password is required for SCRAM SASL mechanism");
        }

        return "org.apache.kafka.common.security.scram.ScramLoginModule required \n" +
               "  username=\"" + username.trim() + "\" \n" +
               "  password=\"" + password.trim() + "\";";
    }

    /**
     * Builds JAAS configuration for GSSAPI (Kerberos) mechanism.
     */
    private String buildGSSAPIJAASConfig(Map<String, String> envVars) {
        String principal = envVars.get("KAFKA_SASL_KERBEROS_PRINCIPAL");
        String keytab = envVars.get("KAFKA_SASL_KERBEROS_KEYTAB");

        StringBuilder jaasConfig = new StringBuilder();
        jaasConfig.append("com.sun.security.auth.module.Krb5LoginModule required \n");

        if (principal != null && !principal.trim().isEmpty()) {
            jaasConfig.append("  principal=\"").append(principal.trim()).append("\" \n");
        }

        if (keytab != null && !keytab.trim().isEmpty()) {
            jaasConfig.append("  useKeyTab=true \n");
            jaasConfig.append("  keyTab=\"").append(keytab.trim()).append("\" \n");
            jaasConfig.append("  storeKey=true \n");
            jaasConfig.append("  useTicketCache=false \n");
        } else {
            jaasConfig.append("  useTicketCache=true \n");
        }

        jaasConfig.append(";");

        return jaasConfig.toString();
    }

    /**
     * Builds JAAS configuration for OAUTHBEARER mechanism.
     */
    private String buildOAuthBearerJAASConfig(Map<String, String> envVars) {
        String token = envVars.get("KAFKA_SASL_OAUTH_TOKEN");
        String tokenEndpoint = envVars.get("KAFKA_SASL_OAUTH_TOKEN_ENDPOINT");

        // For OAUTHBEARER, we use a simple configuration
        // In production, you might want to integrate with a token provider
        StringBuilder jaasConfig = new StringBuilder();
        jaasConfig.append("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required \n");

        if (token != null && !token.trim().isEmpty()) {
            jaasConfig.append("  oauth.token=\"").append(token.trim()).append("\" \n");
        }

        if (tokenEndpoint != null && !tokenEndpoint.trim().isEmpty()) {
            jaasConfig.append("  oauth.token.endpoint=\"").append(tokenEndpoint.trim()).append("\" \n");
        }

        jaasConfig.append(";");

        return jaasConfig.toString();
    }

    /**
     * Adds mechanism-specific properties to the SASL configuration.
     */
    private void addMechanismSpecificProperties(Map<String, Object> saslProperties, String mechanism, Map<String, String> envVars) {
        // Generic SASL properties
        String clientCallbackHandler = envVars.get("KAFKA_SASL_CLIENT_CALLBACK_HANDLER_CLASS");
        if (clientCallbackHandler != null && !clientCallbackHandler.trim().isEmpty()) {
            saslProperties.put("sasl.client.callback.handler.class", clientCallbackHandler.trim());
        }

        String loginCallbackHandler = envVars.get("KAFKA_SASL_LOGIN_CALLBACK_HANDLER_CLASS");
        if (loginCallbackHandler != null && !loginCallbackHandler.trim().isEmpty()) {
            saslProperties.put("sasl.login.callback.handler.class", loginCallbackHandler.trim());
        }

        String loginClass = envVars.get("KAFKA_SASL_LOGIN_CLASS");
        if (loginClass != null && !loginClass.trim().isEmpty()) {
            saslProperties.put("sasl.login.class", loginClass.trim());
        }

        // Mechanism-specific properties
        switch (mechanism) {
            case "GSSAPI":
                addGSSAPIProperties(saslProperties, envVars);
                break;
            case "OAUTHBEARER":
                addOAuthBearerProperties(saslProperties, envVars);
                break;
        }

        // SASL login refresh properties (applicable to multiple mechanisms)
        addLoginRefreshProperties(saslProperties, envVars);
    }

    /**
     * Adds GSSAPI-specific properties.
     */
    private void addGSSAPIProperties(Map<String, Object> saslProperties, Map<String, String> envVars) {
        String serviceName = envVars.get("KAFKA_SASL_KERBEROS_SERVICE_NAME");
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            saslProperties.put("sasl.kerberos.service.name", serviceName.trim());
        }

        String kinitCmd = envVars.get("KAFKA_SASL_KERBEROS_KINIT_CMD");
        if (kinitCmd != null && !kinitCmd.trim().isEmpty()) {
            saslProperties.put("sasl.kerberos.kinit.cmd", kinitCmd.trim());
        }

        String minTimeBeforeRelogin = envVars.get("KAFKA_SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN");
        if (minTimeBeforeRelogin != null && !minTimeBeforeRelogin.trim().isEmpty()) {
            saslProperties.put("sasl.kerberos.min.time.before.relogin", minTimeBeforeRelogin.trim());
        }

        String ticketRenewJitter = envVars.get("KAFKA_SASL_KERBEROS_TICKET_RENEW_JITTER");
        if (ticketRenewJitter != null && !ticketRenewJitter.trim().isEmpty()) {
            saslProperties.put("sasl.kerberos.ticket.renew.jitter", ticketRenewJitter.trim());
        }

        String ticketRenewWindowFactor = envVars.get("KAFKA_SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR");
        if (ticketRenewWindowFactor != null && !ticketRenewWindowFactor.trim().isEmpty()) {
            saslProperties.put("sasl.kerberos.ticket.renew.window.factor", ticketRenewWindowFactor.trim());
        }
    }

    /**
     * Adds OAUTHBEARER-specific properties.
     */
    private void addOAuthBearerProperties(Map<String, Object> saslProperties, Map<String, String> envVars) {
        // OAUTHBEARER mechanism-specific properties can be added here
        // Currently, most OAuth configuration is handled through the JAAS configuration
        LOG.debug("OAUTHBEARER-specific properties configuration");
    }

    /**
     * Adds SASL login refresh properties.
     */
    private void addLoginRefreshProperties(Map<String, Object> saslProperties, Map<String, String> envVars) {
        String refreshBufferSeconds = envVars.get("KAFKA_SASL_LOGIN_REFRESH_BUFFER_SECONDS");
        if (refreshBufferSeconds != null && !refreshBufferSeconds.trim().isEmpty()) {
            saslProperties.put("sasl.login.refresh.buffer.seconds", refreshBufferSeconds.trim());
        }

        String refreshMinPeriodSeconds = envVars.get("KAFKA_SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS");
        if (refreshMinPeriodSeconds != null && !refreshMinPeriodSeconds.trim().isEmpty()) {
            saslProperties.put("sasl.login.refresh.min.period.seconds", refreshMinPeriodSeconds.trim());
        }

        String refreshWindowFactor = envVars.get("KAFKA_SASL_LOGIN_REFRESH_WINDOW_FACTOR");
        if (refreshWindowFactor != null && !refreshWindowFactor.trim().isEmpty()) {
            saslProperties.put("sasl.login.refresh.window.factor", refreshWindowFactor.trim());
        }

        String refreshWindowJitter = envVars.get("KAFKA_SASL_LOGIN_REFRESH_WINDOW_JITTER");
        if (refreshWindowJitter != null && !refreshWindowJitter.trim().isEmpty()) {
            saslProperties.put("sasl.login.refresh.window.jitter", refreshWindowJitter.trim());
        }
    }

    /**
     * Exception thrown when SASL configuration fails.
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
