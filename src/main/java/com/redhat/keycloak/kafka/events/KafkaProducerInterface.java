package com.redhat.keycloak.kafka.events;

import java.util.Map;

import org.apache.kafka.clients.producer.Producer;

public interface KafkaProducerInterface {
    Producer<String, String> createProducer(String clientId, String bootstrapServer,
                                            Map<String, Object> optionalProperties);

}
