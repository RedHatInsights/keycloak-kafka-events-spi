package com.redhat.keycloak.kafka.events;

import java.util.Map;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;

class KafkaMockFactory implements KafkaProducerInterface {

    @Override
    public Producer<String, String> createProducer(String clientId, String bootstrapServer,
                                                   Map<String, Object> optionalProperties) {
        @SuppressWarnings("unchecked")
        Producer<String, String> producer = new MockProducer(
            true, (org.apache.kafka.clients.producer.Partitioner) null,
            new StringSerializer(), new StringSerializer());
        return producer;
    }
}
