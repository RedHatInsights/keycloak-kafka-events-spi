#/bin/bash

KAFKA_TOPIC="${KAFKA_TOPIC:-keycloak-events}"
KAFKA_ADMIN_TOPIC="${KAFKA_ADMIN_TOPIC:-keycloak-admin-events}"
KAFKA_BOOT_SERVER_NAME="${KAFKA_BOOT_SERVER_NAME:-kafka}"
KAFKA_BOOT_SERVER_PORT="${KAFKA_BOOT_SERVER_PORT:-9092}"

for TOPIC in "${KAFKA_TOPIC}" "${KAFKA_ADMIN_TOPIC}"; do
    /opt/bitnami/kafka/bin/kafka-topics.sh --create --topic "${TOPIC}" --bootstrap-server "${KAFKA_BOOT_SERVER_NAME}":"${KAFKA_BOOT_SERVER_PORT}"
    echo "topic "${TOPIC_NAME}" was created"
done
