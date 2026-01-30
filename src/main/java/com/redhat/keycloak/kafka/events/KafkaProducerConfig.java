package com.redhat.keycloak.kafka.events;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config.Scope;

public class KafkaProducerConfig {

    public static Map<String, Object> init(Scope scope) {
        Map<String, Object> propertyMap = new HashMap<>();
        KafkaProducerProperty[] producerProperties = KafkaProducerProperty.values();

        for (KafkaProducerProperty property : producerProperties) {
            String propertyEnv = System.getenv("KAFKA_" + property.name());

            if (property.getName() != null && scope.get(property.getName(), propertyEnv) != null) {
                propertyMap.put(property.getName(), scope.get(property.getName(), propertyEnv));
            }
        }

        return propertyMap;
    }

    enum KafkaProducerProperty {
        // Core Producer Properties
        ACKS("acks"), //
        BUFFER_MEMORY("buffer.memory"), //
        COMPRESSION_TYPE("compression.type"), //
        RETRIES("retries"), //
        BATCH_SIZE("batch.size"), //
        CLIENT_DNS_LOOKUP("client.dns.lookup"), //
        CONNECTION_MAX_IDLE_MS("connections.max.idle.ms"), //
        DELIVERY_TIMEOUT_MS("delivery.timeout.ms"), //
        LINGER_MS("linger.ms"), //
        MAX_BLOCK_MS("max.block.ms"), //
        MAX_REQUEST_SIZE("max.request.size"), //
        PARTITIONER_CLASS("partitioner.class"), //
        RECEIVE_BUFFER_BYTES("receive.buffer.bytes"), //
        REQUEST_TIMEOUT_MS("request.timeout.ms"), //
        SEND_BUFFER_BYTES("send.buffer.bytes"), //
        ENABLE_IDEMPOTENCE("enable.idempotence"), //
        INTERCEPTOR_CLASS("interceptor.classes"), //
        MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION("max.in.flight.requests.per.connection"), //
        METADATA_MAX_AGE_MS("metadata.max.age.ms"), //
        METADATA_MAX_IDLE_MS("metadata.max.idle.ms"), //
        METRIC_REPORTERS("metric.reporters"), //
        METRIC_NUM_SAMPLES("metrics.num.samples"), //
        METRICS_RECORDING_LEVEL("metrics.recording.level"), //
        METRICS_SAMPLE_WINDOW_MS("metrics.sample.window.ms"), //
        RECONNECT_BACKOFF_MAX_MS("reconnect.backoff.max.ms"), //
        RECONNECT_BACKOFF_MS("reconnect.backoff.ms"), //
        RETRY_BACKOFF_MS("retry.backoff.ms"), //
        SECURITY_PROVIDERS("security.providers"), //
        TRANSACTION_TIMEOUT_MS("transaction.timeout.ms"), //
        TRANSACTION_ID("transactional.id"),

        // Security Protocol Configuration
        SECURITY_PROTOCOL("security.protocol"), //

        // SASL Authentication Properties
        SASL_MECHANISM("sasl.mechanism"), //
        SASL_JAAS_CONFIG("sasl.jaas.config"), //
        SASL_CLIENT_CALLBACK_HANDLER_CLASS("sasl.client.callback.handler.class"), //
        SASL_LOGIN_CALLBACK_HANDLER_CLASS("sasl.login.callback.handler.class"), //
        SASL_LOGIN_CLASS("sasl.login.class"), //
        SASL_KERBEROS_SERVICE_NAME("sasl.kerberos.service.name"), //
        SASL_KERBEROS_KINIT_CMD("sasl.kerberos.kinit.cmd"), //
        SASL_KERBEROS_MIN_TIME_BEFORE_RELOGIN("sasl.kerberos.min.time.before.relogin"), //
        SASL_KERBEROS_TICKET_RENEW_JITTER("sasl.kerberos.ticket.renew.jitter"), //
        SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR("sasl.kerberos.ticket.renew.window.factor"), //
        SASL_LOGIN_REFRESH_BUFFER_SECONDS("sasl.login.refresh.buffer.seconds"), //
        SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS("sasl.login.refresh.min.period.seconds"), //
        SASL_LOGIN_REFRESH_WINDOW_FACTOR("sasl.login.refresh.window.factor"), //
        SASL_LOGIN_REFRESH_WINDOW_JITTER("sasl.login.refresh.window.jitter"), //

        // Custom SASL Properties (for environment variable mapping)
        SASL_USERNAME("sasl.username"), //
        SASL_PASSWORD("sasl.password"), //
        SASL_SCRAM_USERNAME("sasl.scram.username"), //
        SASL_SCRAM_PASSWORD("sasl.scram.password"), //
        SASL_KERBEROS_PRINCIPAL("sasl.kerberos.principal"), //
        SASL_KERBEROS_KEYTAB("sasl.kerberos.keytab"), //
        SASL_OAUTH_TOKEN("sasl.oauth.token"), //
        SASL_OAUTH_TOKEN_ENDPOINT("sasl.oauth.token.endpoint"), //

        // Traditional SSL Properties (deprecated, retained for compatibility)
        SSL_KEY_PASSWORD("ssl.key.password"), //
        SSL_ENABLED_PROTOCOLS("ssl.enabled.protocols"), //
        SSL_PROTOCOL("ssl.protocol"), //
        SSL_PROVIDER("ssl.provider"), //
        SSL_CIPHER_SUITES("ssl.cipher.suites"), //
        SSL_ENDPOINT_IDENTIFICATION_ALGORITHM("ssl.endpoint.identification.algorithm"), //
        SSL_KEYMANAGER_ALGORITHM("ssl.keymanager.algorithm"), //
        SSL_SECURE_RANDOM_IMPLEMENTATION("ssl.secure.random.implementation"), //
        SSL_TRUSTMANAGER_ALGORITHM("ssl.trustmanager.algorithm"), //

        // CA Certificate Properties for truststore configuration
        SSL_CA_CERTIFICATE("ssl.ca.certificate"), //
        SSL_TRUSTSTORE_LOCATION("ssl.truststore.location"), //
        SSL_TRUSTSTORE_PASSWORD("ssl.truststore.password"), //
        SSL_TRUSTSTORE_TYPE("ssl.truststore.type"), //

        // Dynamic Certificate Management Properties
        SSL_KEYSTORE_TYPE_DYNAMIC("ssl.keystore.type.dynamic"), //
        SSL_TRUSTSTORE_PASSWORD_DYNAMIC("ssl.truststore.password.dynamic"); //

        private String name;

        private KafkaProducerProperty(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
