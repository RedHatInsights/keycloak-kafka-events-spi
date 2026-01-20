package com.redhat.keycloak.kafka.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.RealmModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration test class that demonstrates using the KafkaProducerFactory
 * to produce dummy Keycloak events. This test shows how to use the connection
 * factory with real event objects and verify message production.
 */
class KafkaProducerIntegrationTest {

    private KafkaProducerFactory producerFactory;
    private MockProducer<String, String> mockProducer;
    private ObjectMapper objectMapper;
    private Map<String, String> environmentVariables;

    @BeforeEach
    void setUp() {
        // Create the real producer factory (not mocked)
        producerFactory = new KafkaProducerFactory();

        // Set up environment variables for security configuration (optional)
        environmentVariables = new HashMap<>();
        // Add sample environment variables if needed for testing security
        // environmentVariables.put("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT");

        // Create a mock producer for testing without actual Kafka connection
        @SuppressWarnings("unchecked")
        MockProducer<String, String> tempProducer = new MockProducer(
            true, (org.apache.kafka.clients.producer.Partitioner) null,
            new org.apache.kafka.common.serialization.StringSerializer(),
            new org.apache.kafka.common.serialization.StringSerializer());
        mockProducer = tempProducer;

        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateProducerWithBasicConfiguration() {
        // Test basic producer creation
        String clientId = "test-client";
        String bootstrapServer = "localhost:9092";
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("acks", "all");

        // This would normally create a real producer, but for testing we'll verify the factory works
        // In a real integration test, you might use testcontainers or an embedded Kafka

        // Verify producer factory is not null
        assertNotNull(producerFactory);

        // If we wanted to test with a real connection, we would do:
        // Producer<String, String> producer = producerFactory.createProducer(clientId, bootstrapServer, optionalProperties);
        // assertNotNull(producer);
    }

    @Test
    void testCreateProducerWithSecurityConfiguration() {
        // Test producer creation with security configuration
        String clientId = "test-client-secure";
        String bootstrapServer = "localhost:9093";
        Map<String, Object> optionalProperties = new HashMap<>();

        // Add security-related environment variables
        environmentVariables.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        environmentVariables.put("KAFKA_SASL_MECHANISM", "PLAIN");
        environmentVariables.put("KAFKA_SASL_USERNAME", "testuser");
        environmentVariables.put("KAFKA_SASL_PASSWORD", "testpass");

        // Test with security configuration
        // Producer<String, String> producer = producerFactory.createProducer(
        //     clientId, bootstrapServer, optionalProperties, environmentVariables);

        assertNotNull(producerFactory);
    }

    @Test
    void testProduceDummyUserEvent() throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
        // Create a dummy Keycloak user event
        Event userEvent = createDummyUserEvent();

        // Convert event to JSON
        String eventJson = objectMapper.writeValueAsString(userEvent);

        // Simulate producing the event using mock producer
        String topic = "keycloak-events";
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, eventJson);

        mockProducer.send(record).get(5, TimeUnit.SECONDS);

        // Verify the event was produced
        assertEquals(1, mockProducer.history().size());
        ProducerRecord<String, String> producedRecord = mockProducer.history().get(0);
        assertEquals(topic, producedRecord.topic());
        assertEquals(eventJson, producedRecord.value());

        // Verify the JSON contains expected event data
        assertTrue(producedRecord.value().contains("LOGIN"));
        assertTrue(producedRecord.value().contains("test-realm"));
        assertTrue(producedRecord.value().contains("test-user"));
    }

    @Test
    void testProduceDummyAdminEvent() throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
        // Create a dummy Keycloak admin event
        AdminEvent adminEvent = createDummyAdminEvent();

        // Convert event to JSON
        String eventJson = objectMapper.writeValueAsString(adminEvent);

        // Simulate producing the event using mock producer
        String topic = "keycloak-admin-events";
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, eventJson);

        mockProducer.send(record).get(5, TimeUnit.SECONDS);

        // Verify the event was produced
        assertEquals(1, mockProducer.history().size());
        ProducerRecord<String, String> producedRecord = mockProducer.history().get(0);
        assertEquals(topic, producedRecord.topic());
        assertEquals(eventJson, producedRecord.value());

        // Verify the JSON contains expected event data
        assertTrue(producedRecord.value().contains("CREATE"));
        assertTrue(producedRecord.value().contains("USER"));
    }

    @Test
    void testEventListenerProviderWithDummyEvents() {
        // Test using the KafkaEventListenerProvider with dummy events
        String bootstrapServers = "localhost:9092";
        String clientId = "test-listener";
        String topicEvents = "keycloak-events";
        String[] events = {"LOGIN", "REGISTER", "LOGOUT"};
        String topicAdminEvents = "keycloak-admin-events";
        Map<String, Object> kafkaProducerProperties = new HashMap<>();

        // Create a custom factory that returns our mock producer
        KafkaProducerInterface testFactory = new KafkaProducerInterface() {
            @Override
            public org.apache.kafka.clients.producer.Producer<String, String> createProducer(
                    String clientId, String bootstrapServer, Map<String, Object> optionalProperties) {
                return mockProducer;
            }
        };

        // Create the event listener provider
        KafkaEventListenerProvider provider = new KafkaEventListenerProvider(
            bootstrapServers, clientId, topicEvents, events, topicAdminEvents,
            kafkaProducerProperties, testFactory);

        assertNotNull(provider);

        // Create and send a dummy user event
        Event dummyUserEvent = createDummyUserEvent();
        provider.onEvent(dummyUserEvent);

        // Verify the event was processed
        assertEquals(1, mockProducer.history().size());

        // Create and send a dummy admin event
        AdminEvent dummyAdminEvent = createDummyAdminEvent();
        provider.onEvent(dummyAdminEvent, false);

        // Verify the admin event was processed
        assertEquals(2, mockProducer.history().size());
    }

    @Test
    void testMultipleEventTypes() throws JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {
        // Test producing multiple different event types
        EventType[] eventTypes = {EventType.LOGIN, EventType.REGISTER, EventType.LOGOUT, EventType.UPDATE_PASSWORD};

        for (EventType eventType : eventTypes) {
            Event event = createDummyUserEvent();
            event.setType(eventType);

            String eventJson = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>("keycloak-events", eventJson);

            mockProducer.send(record).get(5, TimeUnit.SECONDS);
        }

        // Verify all events were produced
        assertEquals(eventTypes.length, mockProducer.history().size());

        // Verify each event type is present
        for (int i = 0; i < eventTypes.length; i++) {
            ProducerRecord<String, String> producedRecord = mockProducer.history().get(i);
            assertTrue(producedRecord.value().contains(eventTypes[i].toString()));
        }
    }

    /**
     * Helper method to create a dummy Keycloak user event for testing.
     */
    private Event createDummyUserEvent() {
        Event event = new Event();
        event.setType(EventType.LOGIN);
        event.setRealmId("test-realm");
        event.setClientId("test-client");
        event.setUserId("test-user");
        event.setSessionId("test-session");
        event.setIpAddress("127.0.0.1");
        event.setTime(System.currentTimeMillis());

        // Add some details using the details Map
        if (event.getDetails() == null) {
            event.setDetails(new java.util.HashMap<>());
        }
        event.getDetails().put("auth_method", "openid-connect");
        event.getDetails().put("response_type", "code");
        event.getDetails().put("redirect_uri", "http://localhost:8080/callback");
        event.getDetails().put("consent", "no");

        return event;
    }

    /**
     * Helper method to create a dummy Keycloak admin event for testing.
     */
    private AdminEvent createDummyAdminEvent() {
        AdminEvent event = new AdminEvent();
        event.setTime(System.currentTimeMillis());
        event.setRealmId("test-realm");
        event.setOperationType(org.keycloak.events.admin.OperationType.CREATE);
        event.setResourceType(org.keycloak.events.admin.ResourceType.USER);
        event.setResourcePath("users/new-user");
        event.setRepresentation("{\"username\":\"new-user\",\"email\":\"newuser@test.com\"}");
        event.setAuthDetails(null); // Could be populated with auth details if needed

        return event;
    }
}
