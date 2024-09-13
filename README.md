[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# Keycloak Events SPI

A [Service Provider](https://www.keycloak.org/docs/latest/server_development/index.html#_providers) That will push 
admin events to a specified Kafka topic for consumption.

provider(s) are defined:

* KafkaEventListenerProvider to record the Keycloak events and push to Kafka topic

## License 

 See [LICENSE file](./LICENSE)

## Build

Build the project using:
 * [Maven](https://maven.apache.org/)

The project is packaged as a jar file and bundles the prometheus client libraries.

### Maven

To build the jar file using maven run the following command (will bundle the prometheus client libraries as well):

```sh
  mvn package
```

Run tests

```sh
  mvn test
```

It will build the project and write jar to the _./target_.

### Configurable versions for some packages

You can build the project using a different version of Keycloak or kafka, running the command:

#### For Maven

```sh
mvn clean package -Dkeycloak.version=15.0.0 -Dkafka.version=3.7.0
```

## Install and setup

### On Keycloak Quarkus Distribution

> We assume the home of keycloak is on the default `/opt/keycloak`

You will need to either copy the `jar` into the build step and run step, or copy it from the build stage. Following the [example docker instructions](https://www.keycloak.org/server/containers)
No need to add `.dodeploy`.

```
# On build stage
COPY keycloak-events-spi.jar /opt/keycloak/providers/

# On run stage
COPY keycloak-events-spi.jar /opt/keycloak/providers/

```
If not copied to both stages keycloak will complain 
```
ERROR: Failed to open /opt/keycloak/lib/../providers/keycloak-events-spi.jar
```

### Enable Events in keycloak
1. Open administration console
2. Choose realm
3. Go to Events
4. Open `Config` tab and add `kafka` to Event Listeners.

![Admin console config](images/initialize-kafka-listener.png)

## Podman/Docker Container
The simplest way to enable the kafka module in a docker container is to create a custom docker image from the keycloak 
base image. A simple example can be found in the [Dockerfile](Dockerfile).
When you build this image on your local machine by using 
```sh
podman build . -t keycloak-kafka --tls-verify=false
```

You can test everything by running the [docker-compose](docker-compose.yml) file on your local machine.
Note: Make sure you have set up Podman to work with [compose](https://podman-desktop.io/docs/compose/setting-up-compose). 
```sh
podman compose --file docker-compose.yml up --detach
```


