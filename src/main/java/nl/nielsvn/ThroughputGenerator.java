package nl.nielsvn;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A component that generates a stream of MQTT messages to test throughput.
 * <p>
 * This class is responsible for creating a configurable number of messages per second, each with a specific payload size,
 * and sending them to a configured MQTT broker. It also provides statistics on the message rate and bandwidth.
 */
@ApplicationScoped
public class ThroughputGenerator {
    private static final Logger LOG = Logger.getLogger(ThroughputGenerator.class);

    @ConfigProperty(name = "throughput.payload-size", defaultValue = "1024")
    int payloadSize;

    @ConfigProperty(name = "throughput.messages-per-second", defaultValue = "1000")
    long messagesPerSecond;

    @ConfigProperty(name = "throughput.stats.enabled", defaultValue = "true")
    boolean statsEnabled;

    @ConfigProperty(name = "mp.messaging.outgoing.mqtt-throughput.host")
    String brokerHost;

    @ConfigProperty(name = "mp.messaging.outgoing.mqtt-throughput.port")
    int brokerPort;

    @ConfigProperty(name = "mp.messaging.outgoing.mqtt-throughput.topic")
    String topic;

    long statsBatchSize;

    private byte[] payload;
    private final AtomicLong counter = new AtomicLong(0);
    private long lastTime = System.currentTimeMillis();

    @PostConstruct
    void init() {
        this.payload = createPayload(payloadSize);
        if (messagesPerSecond <= 0) {
            LOG.warnf("Invalid throughput.messages-per-second=%d; falling back to 1 msg/s", messagesPerSecond);
            messagesPerSecond = 1;
        }
        this.statsBatchSize = messagesPerSecond;
        this.lastTime = System.currentTimeMillis();

        LOG.infof(
                "Starting MQTT throughput generator: broker=%s:%d, topic=%s, payload=%d bytes, throughput=%d msg/s, stats=%s",
                brokerHost, brokerPort, topic, payload.length, messagesPerSecond, statsEnabled
        );
    }

    /**
     * Generates a continuous stream of MQTT messages.
     * <p>
     * This method produces a {@link Multi} of {@link Message}s at a rate determined by the {@code messagesPerSecond}
     * configuration property. Each message contains a randomly generated payload.
     *
     * @return A {@link Multi} of MQTT messages.
     */
    @Outgoing("mqtt-throughput")
    public Multi<Message<byte[]>> generateStream() {
        // Calculate the interval between messages to achieve the desired throughput.
        Duration interval = Duration.ofNanos(Math.max(1, 1_000_000_000L / messagesPerSecond));

        return Multi.createFrom().ticks().every(interval)
                .onOverflow().drop()
                .map(tick -> Message.of(payload).withAck(this::handleMessageAcknowledgement))
                .onFailure().invoke(throwable -> LOG.error("MQTT throughput stream failed", throwable));
    }

    /**
     * Handles the acknowledgement of a sent message and triggers statistics printing.
     *
     * @return A {@link CompletableFuture} that completes when the acknowledgement is handled.
     */
    private CompletableFuture<Void> handleMessageAcknowledgement() {
        long currentCount = counter.incrementAndGet();
        if (statsEnabled && statsBatchSize > 0 && currentCount % statsBatchSize == 0) {
            printStats(currentCount);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Prints throughput statistics to the log.
     *
     * @param totalCount The total number of messages sent so far.
     */
    private void printStats(long totalCount) {
        long now = System.currentTimeMillis();
        long diff = now - lastTime;

        if (diff > 0) {
            double rate = (statsBatchSize / (double) diff) * 1000;
            double throughputMB = (rate * payload.length) / (1024 * 1024);

            LOG.infof("Total: %d | Batch: %d in %d ms | Rate: %.0f msg/s | Bandwidth: %.2f MB/s",
                    totalCount, statsBatchSize, diff, rate, throughputMB);

            lastTime = now;
        }
    }

    /**
     * Creates a byte array of a given size with random content.
     *
     * @param size The size of the byte array to create.
     * @return A new byte array filled with random data.
     */
    private static byte[] createPayload(int size) {
        byte[] bytes = new byte[Math.max(0, size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}