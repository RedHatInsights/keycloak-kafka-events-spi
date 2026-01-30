#!/bin/bash

# Simple script to run the Kafka Consumer-Producer Test with security configuration

export KAFKA_BOOTSTRAP_SERVERS="kafka-host-with-ssl:9093"
export KAFKA_SECURITY_PROTOCOL="SASL_SSL"
export KAFKA_TEST_TOPIC="keycloak-events"
export KAFKA_SASL_MECHANISM="SCRAM-SHA-512"
export KAFKA_SASL_USERNAME="kafka-user-all"
export KAFKA_SASL_PASSWORD="changeme"
export KAFKA_SSL_CA_CERTIFICATE="$(cat /path/to/cert.pem)" # this comes direct from kafka - same cert.
export KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=""

# Note: -Djava.security.manager=allow flag removed for Java 25+
# The Security Manager was completely removed in Java 25.
# SASL authentication now works without this flag in Kafka 3.x+
# See: JEP 486 - Deprecate the Security Manager for Removal

mvn exec:java -Dexec.mainClass="com.redhat.keycloak.kafka.events.KafkaConsumerProducerTest"
