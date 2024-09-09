package com.redhat.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AdminEventProvider implements EventListenerProvider, EventListenerProviderFactory {

    private KafkaEventProducer kafkaProducer;

    @Override
    public void onEvent(Event event) {
        // Convert event to JSON or string format
        String eventJson = convertEventToJson(event);

        // Send event to Kafka topic
        kafkaProducer.sendEvent("keycloak-events", eventJson);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {
        // Cleanup resources
        kafkaProducer.close();
    }

    @Override
    public void init(Config.Scope config) {
        // Initialize Kafka producer
        kafkaProducer = new KafkaEventProducer();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No implementation needed
    }

    @Override
    public String getId() {
        return "custom-kafka-event-listener";
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return this;
    }

    private String convertEventToJson(Event event) {
        // Convert Keycloak Event object to JSON format
        // Example implementation:
        // return new Gson().toJson(event);
        return event.toString(); // Simplified for demonstration
    }
}
