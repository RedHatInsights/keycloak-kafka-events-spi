package com.redhat.keycloak.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaEventProducer {
    private final Producer<String, String> producer;

    public KafkaEventProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "my-cluster-kafka-bootstrap:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
    }

    public void sendEvent(String topic, String event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event);
        producer.send(record);
    }

    public void close() {
        producer.close();
    }
}
