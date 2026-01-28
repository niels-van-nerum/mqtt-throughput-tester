# MQTT Throughput Tester

A high-performance MQTT throughput testing tool built with Quarkus and SmallRye Reactive Messaging. This application generates a configurable stream of MQTT messages to test the throughput and performance of MQTT brokers.

## Features

- **Configurable throughput**: Set the desired messages per second rate
- **Adjustable payload size**: Configure message payload size in bytes
- **Real-time statistics**: Live monitoring of message rates and bandwidth
- **Reactive architecture**: Built on SmallRye Reactive Messaging for high performance
- **MQTT 3.1.1 support**: Compatible with any MQTT 3.1.1 broker
- **Flexible deployment**: Run as JVM application, native executable, or Docker container
- **Quality of Service**: Configurable QoS levels (default: QoS 0 for maximum throughput)

## Prerequisites

- Java 21 or later
- Maven 3.8+ (or use the included Maven wrapper)
- An MQTT broker (e.g., Mosquitto, HiveMQ, EMQX)

## Configuration

The application is configured through `src/main/resources/application.properties`. Key configuration parameters:

### MQTT Broker Settings

```properties
mp.messaging.outgoing.mqtt-throughput.host=localhost
mp.messaging.outgoing.mqtt-throughput.port=1883
mp.messaging.outgoing.mqtt-throughput.username=
mp.messaging.outgoing.mqtt-throughput.password=
mp.messaging.outgoing.mqtt-throughput.topic=test/throughput
```

### Throughput Settings

```properties
# Payload size in bytes (default: 1024)
throughput.payload-size=1024

# Target messages per second (default: 1000)
throughput.messages-per-second=1000

# Enable/disable statistics logging (default: true)
throughput.stats.enabled=true
```

### Advanced MQTT Settings

```properties
# Auto-generate unique client ID for each run
mp.messaging.outgoing.mqtt-throughput.auto-generated-client-id=true

# Maximum number of messages in the inflight queue (default: 50000)
mp.messaging.outgoing.mqtt-throughput.max-inflight-queue=50000

# Quality of Service level: 0, 1, or 2 (default: 0)
mp.messaging.outgoing.mqtt-throughput.qos=0
```

## Building the Application

### Standard JAR

```bash
./mvnw package
```

This produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.

### Uber JAR

To create a self-contained JAR with all dependencies:

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

### Native Executable

For maximum performance, create a native executable:

```bash
# With GraalVM installed
./mvnw package -Dnative

# Using container build (no GraalVM installation required)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Running the Application

### Development Mode

Run with live coding and auto-reload:

```bash
./mvnw quarkus:dev
```

The Quarkus Dev UI is available at <http://localhost:8080/q/dev/>.

### Production Mode

#### From Standard JAR

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

#### From Uber JAR

```bash
java -jar target/*-runner.jar
```

#### Native Executable

```bash
./target/test-smallrye-throughput-1.0-SNAPSHOT-runner
```

### With Custom Configuration

Override configuration properties at runtime:

```bash
java -jar target/quarkus-app/quarkus-run.jar \
  -Dmp.messaging.outgoing.mqtt-throughput.host=mqtt.example.com \
  -Dmp.messaging.outgoing.mqtt-throughput.port=1883 \
  -Dmp.messaging.outgoing.mqtt-throughput.topic=test/load \
  -Dthroughput.payload-size=2048 \
  -Dthroughput.messages-per-second=5000
```

## Docker Deployment

### Building Docker Images

Several Dockerfiles are provided for different deployment scenarios:

#### JVM Container (Recommended for Development)

```bash
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t mqtt-throughput-tester:jvm .
```

#### Legacy JAR Container

```bash
./mvnw package
docker build -f src/main/docker/Dockerfile.legacy-jar -t mqtt-throughput-tester:legacy-jar .
```

#### Native Container (Recommended for Production)

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t mqtt-throughput-tester:native .
```

#### Native Micro Container (Minimal Footprint)

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native-micro -t mqtt-throughput-tester:native-micro .
```

### Running in Docker

```bash
docker run -e MP_MESSAGING_OUTGOING_MQTT_THROUGHPUT_HOST=mqtt-broker \
  -e MP_MESSAGING_OUTGOING_MQTT_THROUGHPUT_PORT=1883 \
  -e MP_MESSAGING_OUTGOING_MQTT_THROUGHPUT_TOPIC=test/throughput \
  -e THROUGHPUT_PAYLOAD_SIZE=1024 \
  -e THROUGHPUT_MESSAGES_PER_SECOND=1000 \
  mqtt-throughput-tester:jvm
```

## Usage Example

1. **Start an MQTT broker** (if you don't have one):

   ```bash
   docker run -d -p 1883:1883 eclipse-mosquitto:2.0
   ```

2. **Configure the application** by editing `src/main/resources/application.properties`:

   ```properties
   mp.messaging.outgoing.mqtt-throughput.host=localhost
   mp.messaging.outgoing.mqtt-throughput.port=1883
   mp.messaging.outgoing.mqtt-throughput.topic=test/throughput
   throughput.payload-size=1024
   throughput.messages-per-second=1000
   ```

3. **Run the application**:

   ```bash
   ./mvnw quarkus:dev
   ```

4. **Monitor the output** for real-time statistics:

   ```
   Total: 1000 | Batch: 1000 in 995 ms | Rate: 1005 msg/s | Bandwidth: 0.98 MB/s
   Total: 2000 | Batch: 1000 in 997 ms | Rate: 1003 msg/s | Bandwidth: 0.98 MB/s
   Total: 3000 | Batch: 1000 in 998 ms | Rate: 1002 msg/s | Bandwidth: 0.98 MB/s
   ```

## Output Statistics

The application logs periodic statistics showing:

- **Total**: Total number of messages sent
- **Batch**: Number of messages in the current batch and time taken
- **Rate**: Actual message rate (messages per second)
- **Bandwidth**: Data throughput (MB/s)

Statistics are logged every `messages-per-second` messages by default.

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── docker/                           # Dockerfiles for different deployment modes
│   │   │   ├── Dockerfile.jvm                # JVM-based container
│   │   │   ├── Dockerfile.legacy-jar         # Legacy JAR container
│   │   │   ├── Dockerfile.native             # Native executable container
│   │   │   └── Dockerfile.native-micro       # Micro native container
│   │   ├── java/
│   │   │   └── nl/nielsvn/
│   │   │       └── ThroughputGenerator.java  # Main application logic
│   │   └── resources/
│   │       └── application.properties        # Configuration
│   └── test/
│       └── java/
│           └── nl/nielsvn/
│               └── ThroughputGeneratorTest.java  # Unit tests
├── mvnw                                  # Maven wrapper script (Unix)
├── mvnw.cmd                              # Maven wrapper script (Windows)
├── pom.xml                               # Maven project configuration
└── README.md                             # This file
```

## Testing

The project includes comprehensive unit tests for the `ThroughputGenerator` component.

### Running Tests

Execute the test suite using Maven:

```bash
./mvnw test
```

Or with the standard Maven command:

```bash
mvn test
```

### Test Coverage

The test suite (`ThroughputGeneratorTest.java`) includes tests for:

- Configuration initialization and validation
- Payload generation with various sizes
- Message stream creation and emission
- Message acknowledgment and counter increments
- Statistics batch size configuration
- Edge cases (zero, negative, and extreme values)

### Integration Tests

Integration tests can be run with:

```bash
./mvnw verify
```

Note: Integration tests are skipped by default (`skipITs=true` in `pom.xml`) and are enabled in the native profile.

## Technology Stack

- **Quarkus 3.30.2**: Supersonic Subatomic Java Framework
- **SmallRye Reactive Messaging**: Reactive messaging framework
- **MQTT**: Eclipse Paho MQTT client via SmallRye
- **Java 21**: Latest LTS Java version
- **JUnit 5**: Testing framework
- **SmallRye Mutiny**: Reactive programming library

## Performance Tips

1. **Use QoS 0** for maximum throughput (default setting)
2. **Increase inflight queue size** for high-throughput scenarios
3. **Use native executable** for best performance
4. **Tune JVM settings** when running in JVM mode:
   ```bash
   java -XX:+UseG1GC -Xmx512m -jar target/quarkus-app/quarkus-run.jar
   ```
5. **Adjust batch size** by changing `messages-per-second` to match your target rate

## Troubleshooting

### Connection Issues

If the application fails to connect to the MQTT broker:

- Verify the broker host and port are correct
- Check firewall and network connectivity
- Ensure the broker is running and accepting connections
- Verify username/password if authentication is required

### Performance Issues

If the actual throughput is lower than configured:

- Check broker capacity and configuration
- Increase the `max-inflight-queue` setting
- Monitor broker CPU and memory usage
- Consider using native executable for better performance
- Ensure network bandwidth is sufficient

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is available under the Apache License 2.0.

## Learn More

- [Quarkus](https://quarkus.io/)
- [SmallRye Reactive Messaging](https://smallrye.io/smallrye-reactive-messaging/)
- [MQTT Protocol](https://mqtt.org/)
- [Eclipse Paho](https://www.eclipse.org/paho/)
