package com.redhat.keycloak.kafka.events;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jboss.logging.Logger;

/**
 * Standalone test class that uses KafkaProducerFactory to:
 * 1. Connect to a Kafka instance and list available topics
 * 2. Spin up a consumer listener on a specific topic
 * 3. Produce a message to that topic
 * 4. Exit once the message is received back
 *
 * This can be run as a standalone application for manual testing.
 */
public class KafkaConsumerProducerTest {

    private static final Logger LOG = Logger.getLogger(KafkaConsumerProducerTest.class);

    public static void main(String[] args) {
        // Configuration - can be overridden via environment variables or command line args
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String topic = System.getenv().getOrDefault("KAFKA_TEST_TOPIC", "keycloak-events");
        String clientId = "kafka-test-client";
        String testMessage = "Kafka test message sent at: " + System.currentTimeMillis();

        // Security configuration from environment variables
        Map<String, String> securityEnvVars = getSecurityEnvironmentVariables();

        LOG.info("Starting Kafka Consumer-Producer Test");
        LOG.info("Bootstrap Servers: " + bootstrapServers);
        LOG.info("Topic: " + topic);

        try {
            // // Step 1: Create producer using KafkaProducerFactory
            // LOG.info("Step 1: Creating producer using KafkaProducerFactory...");
            // KafkaProducerFactory producerFactory = new KafkaProducerFactory();
            // Producer<String, String> producer = producerFactory.createProducer(
            //         clientId,
            //         bootstrapServers,
            //         new HashMap<>(),
            //         securityEnvVars
            // );
            // LOG.info("Producer created successfully");

            // Step 2: List available topics
            LOG.info("Step 2: Listing available topics...");
            listTopics(bootstrapServers, securityEnvVars);

            // // Step 3: Verify topic exists
            // LOG.info("Step 3: Verifying topic '" + topic + "' exists...");
            // boolean topicExists = verifyTopicExists(bootstrapServers, topic, securityEnvVars);
            // if (!topicExists) {
            //     LOG.error("Topic '" + topic + "' does not exist. Please create it first.");
            //     System.exit(1);
            // }
            // LOG.info("Topic verified: " + topic);

            // // Step 4: Set up consumer listener
            // LOG.info("Step 4: Setting up consumer listener...");
            // CountDownLatch messageReceivedLatch = new CountDownLatch(1);
            // AtomicReference<String> receivedMessage = new AtomicReference<>();

            // Thread consumerThread = startConsumerThread(
            //         bootstrapServers,
            //         topic,
            //         clientId + "-consumer",
            //         securityEnvVars,
            //         messageReceivedLatch,
            //         receivedMessage
            // );

            // // Wait a moment for consumer to be ready
            // Thread.sleep(2000);

            // // Step 5: Produce message
            // LOG.info("Step 5: Producing message to topic '" + topic + "'");
            // LOG.info("Message content: " + testMessage);
            // ProducerRecord<String, String> record = new ProducerRecord<>(topic, testMessage);
            // RecordMetadata metadata = producer.send(record).get(30, TimeUnit.SECONDS);
            // LOG.info("Message sent successfully");
            // LOG.info("  Topic: " + metadata.topic());
            // LOG.info("  Partition: " + metadata.partition());
            // LOG.info("  Offset: " + metadata.offset());

            // // Step 6: Wait for message to be received
            // LOG.info("Step 6: Waiting for message to be received by consumer...");
            // boolean received = messageReceivedLatch.await(30, TimeUnit.SECONDS);

            // if (received) {
            //     LOG.info("SUCCESS: Message received by consumer!");
            //     LOG.info("Received message: " + receivedMessage.get());

            //     // Verify the message matches
            //     if (testMessage.equals(receivedMessage.get())) {
            //         LOG.info("Message content matches - test PASSED!");
            //     } else {
            //         LOG.warn("Message content does not match!");
            //         LOG.warn("  Sent: " + testMessage);
            //         LOG.warn("  Received: " + receivedMessage.get());
            //     }
            // } else {
            //     LOG.error("FAILED: Message not received within timeout");
            //     System.exit(1);
            // }

            // // Cleanup
            // LOG.info("Cleaning up resources...");
            // producer.close();
            // consumerThread.interrupt();
            // consumerThread.join(5000);

            LOG.info("Test completed successfully!");
            System.exit(0);

        } catch (Exception e) {
            LOG.error("Test failed with exception", e);
            System.exit(1);
        }
    }

    /**
     * Get security-related environment variables for Kafka configuration.
     */
    private static Map<String, String> getSecurityEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();

        // Copy all KAFKA_ prefixed environment variables
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith("KAFKA_")) {
                envVars.put(entry.getKey(), entry.getValue());
            }
        }

        return envVars;
    }

    /**
     * List all available topics in the Kafka cluster.
     */
    private static void listTopics(String bootstrapServers, Map<String, String> securityEnvVars) {
        try (Consumer<String, String> consumer = createConsumer(bootstrapServers, "list-topics-client", securityEnvVars)) {

            // Get topic metadata
            Map<String, List<PartitionInfo>> topics = consumer.listTopics();

            LOG.info("Available topics:");
            for (String topicName : topics.keySet()) {
                int partitionCount = topics.get(topicName).size();
                LOG.info("  - " + topicName + " (" + partitionCount + " partition(s))");
            }

            LOG.info("Total topics: " + topics.size());

        } catch (Exception e) {
            LOG.error("Failed to list topics", e);
            throw new RuntimeException("Failed to list topics: " + e.getMessage(), e);
        }
    }

    /**
     * Verify if a specific topic exists.
     */
    private static boolean verifyTopicExists(String bootstrapServers, String topic,
                                                Map<String, String> securityEnvVars) {
        try (Consumer<String, String> consumer = createConsumer(bootstrapServers, "verify-topic-client", securityEnvVars)) {

            Map<String, List<PartitionInfo>> topics = consumer.listTopics();
            return topics.containsKey(topic);

        } catch (Exception e) {
            LOG.error("Failed to verify topic existence", e);
            return false;
        }
    }

    /**
     * Create a Kafka consumer with proper security configuration.
     */
    private static Consumer<String, String> createConsumer(String bootstrapServers,
                                                            String clientId,
                                                            Map<String, String> securityEnvVars) {
        Properties props = new Properties();

        // Basic consumer configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, clientId + "-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Apply security configuration from environment variables
        if (!securityEnvVars.isEmpty()) {
            try {
                SecurityConfiguration securityConfig = new SecurityConfiguration(securityEnvVars);
                securityConfig.validateConfiguration();
                Map<String, Object> securityProperties = securityConfig.getSecurityProperties();
                props.putAll(securityProperties);
                LOG.debug("Security configuration applied to consumer: " + securityProperties.get("security.protocol"));
            } catch (Exception e) {
                LOG.warn("Failed to apply security configuration to consumer", e);
            }
        }

        return new KafkaConsumer<>(props);
    }

    /**
     * Start a consumer thread that listens for messages on the specified topic.
     */
    private static Thread startConsumerThread(String bootstrapServers,
                                               String topic,
                                               String clientId,
                                               Map<String, String> securityEnvVars,
                                               CountDownLatch messageReceivedLatch,
                                               AtomicReference<String> receivedMessage) {

        Thread consumerThread = new Thread(() -> {
            try (Consumer<String, String> consumer = createConsumer(bootstrapServers, clientId, securityEnvVars)) {

                LOG.info("Consumer thread started, subscribing to topic: " + topic);
                consumer.subscribe(Collections.singletonList(topic));

                // Poll for messages
                while (!Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        LOG.info("Received message:");
                        LOG.info("  Topic: " + record.topic());
                        LOG.info("  Partition: " + record.partition());
                        LOG.info("  Offset: " + record.offset());
                        LOG.info("  Key: " + record.key());
                        LOG.info("  Value: " + record.value());

                        receivedMessage.set(record.value());
                        messageReceivedLatch.countDown();

                        // Exit after receiving the message
                        return;
                    }
                }

            } catch (Exception e) {
                if (!Thread.currentThread().isInterrupted()) {
                    LOG.error("Consumer thread error", e);
                }
            }
        }, "kafka-consumer-thread");

        consumerThread.setDaemon(true);
        consumerThread.start();

        return consumerThread;
    }
}
