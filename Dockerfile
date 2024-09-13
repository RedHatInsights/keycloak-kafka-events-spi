FROM quay.io/keycloak/keycloak:23.0.0

COPY ./target/keycloak-kafka-events-spi-1.0.0-SNAPSHOT-jar-with-dependencies.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
