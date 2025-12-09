# MQTT Throughput Tester

This project is a Quarkus-based application designed to generate a configurable stream of MQTT messages to test the throughput of an MQTT broker. It provides a simple REST API to start and stop the message generation.

## Overview

The application consists of two main parts:

*   **ThroughputGenerator**: A component that generates MQTT messages at a configurable rate.
*   **ThroughputResource**: A JAX-RS resource that exposes a REST API to control the generator.

## Configuration

The application can be configured via the `src/main/resources/application.properties` file. The following properties are available:

| Property                              | Description                                       | Default Value |
| ------------------------------------- | ------------------------------------------------- | ------------- |
| `throughput.payload-size`             | The size of the message payload in bytes.         | `1024`        |
| `throughput.messages-per-second`      | The number of messages to send per second.        | `1000`        |
| `throughput.stats.enabled`            | Enable or disable the logging of statistics.      | `true`        |
| `mp.messaging.outgoing.mqtt-throughput.host` | The hostname of the MQTT broker.                  | `localhost`   |
| `mp.messaging.outgoing.mqtt-throughput.port` | The port of the MQTT broker.                      | `1883`        |
| `mp.messaging.outgoing.mqtt-throughput.topic`| The MQTT topic to which messages are published.   | `throughput`  |

## API

The following endpoints are available to control the throughput generator:

*   `POST /throughput/start`: Starts the message generation.
*   `POST /throughput/stop`: Stops the message generation.
*   `GET /throughput/status`: Returns the current status of the generator (running or not).

### Example

```shell
# Start the generator
curl -X POST http://localhost:8080/throughput/start

# Check the status
curl http://localhost:8080/throughput/status

# Stop the generator
curl -X POST http://localhost:8080/throughput/stop
```

## Running the application

You can run the application in development mode with live coding using:

```shell
./mvnw quarkus:dev
```

The application will be available at `http://localhost:8080`.