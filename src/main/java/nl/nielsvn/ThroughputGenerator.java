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
                brokerHost, brokerPort, topic, payload.length, messagesPerSecond, statsEnabled, statsBatchSize
        );
    }

    @Outgoing("mqtt-throughput")
    public Multi<Message<byte[]>> generateStream() {
        Duration interval = Duration.ofNanos(Math.max(1, (long) (1_000_000_000L / messagesPerSecond)));

        return Multi.createFrom().ticks().every(interval)
                .onOverflow().drop()
                .map(tick -> Message.of(payload)
                        .withAck(() -> {
                            long c = counter.incrementAndGet();
                            if (statsEnabled && statsBatchSize > 0 && c % statsBatchSize == 0) {
                                printStats(c);
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                )
                .onFailure().invoke(t -> LOG.error("MQTT throughput stream failed", t));
    }

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

    private static byte[] createPayload(int size) {
        byte[] b = new byte[Math.max(0, size)];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }
}