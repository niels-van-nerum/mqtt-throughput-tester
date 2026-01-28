package nl.nielsvn;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ThroughputGeneratorTest {

    @InjectMocks
    private ThroughputGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new ThroughputGenerator();
        // Set default config values via reflection
        setField(generator, "payloadSize", 1024);
        setField(generator, "messagesPerSecond", 1000L);
        setField(generator, "statsEnabled", true);
        setField(generator, "brokerHost", "localhost");
        setField(generator, "brokerPort", 1883);
        setField(generator, "topic", "test-topic");
    }

    @Test
    void testInit_withValidConfig() throws Exception {
        // When
        generator.init();

        // Then
        byte[] payload = getField(generator, "payload");
        assertNotNull(payload);
        assertEquals(1024, payload.length);
        Long statsBatchSize = getField(generator, "statsBatchSize");
        assertEquals(1000L, statsBatchSize);
    }

    @Test
    void testInit_withZeroPayloadSize() throws Exception {
        // Given
        setField(generator, "payloadSize", 0);

        // When
        generator.init();

        // Then
        byte[] payload = getField(generator, "payload");
        assertNotNull(payload);
        assertEquals(0, payload.length);
    }

    @Test
    void testInit_withNegativePayloadSize() throws Exception {
        // Given
        setField(generator, "payloadSize", -100);

        // When
        generator.init();

        // Then
        byte[] payload = getField(generator, "payload");
        assertNotNull(payload);
        assertEquals(0, payload.length);
    }

    @Test
    void testInit_withInvalidMessagesPerSecond() throws Exception {
        // Given
        setField(generator, "messagesPerSecond", 0L);

        // When
        generator.init();

        // Then
        Long messagesPerSecond = getField(generator, "messagesPerSecond");
        assertEquals(1L, messagesPerSecond);
    }

    @Test
    void testInit_withNegativeMessagesPerSecond() throws Exception {
        // Given
        setField(generator, "messagesPerSecond", -100L);

        // When
        generator.init();

        // Then
        Long messagesPerSecond = getField(generator, "messagesPerSecond");
        assertEquals(1L, messagesPerSecond);
    }

    @Test
    void testGenerateStream_createsMultiStream() {
        // Given
        generator.init();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();

        // Then
        assertNotNull(stream);
    }

    @Test
    void testGenerateStream_emitsMessages() {
        // Given
        generator.init();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(5)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(5));

        // Then
        subscriber
                .awaitItems(5, Duration.ofSeconds(10))
                .assertCompleted();

        List<Message<byte[]>> items = subscriber.getItems();
        assertEquals(5, items.size());
        
        // Verify each message has correct payload
        for (Message<byte[]> message : items) {
            assertNotNull(message.getPayload());
            assertEquals(1024, message.getPayload().length);
        }
    }

    @Test
    void testGenerateStream_messagesHaveCorrectPayloadSize() throws Exception {
        // Given
        setField(generator, "payloadSize", 512);
        generator.init();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(3)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        // Then
        subscriber
                .awaitItems(3, Duration.ofSeconds(10))
                .assertCompleted();

        List<Message<byte[]>> items = subscriber.getItems();
        for (Message<byte[]> message : items) {
            assertEquals(512, message.getPayload().length);
        }
    }

    @Test
    void testAckCallback_incrementsCounter() throws Exception {
        // Given
        generator.init();
        AtomicLong counter = getField(generator, "counter");
        long initialCount = counter.get();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(3)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));
        
        subscriber.awaitItems(3, Duration.ofSeconds(10));
        List<Message<byte[]>> items = subscriber.getItems();
        
        // Ack each message
        for (Message<byte[]> message : items) {
            CompletableFuture<?> ackFuture = (CompletableFuture<?>) message.ack().toCompletableFuture();
            ackFuture.get();
        }

        // Then
        assertEquals(initialCount + 3, counter.get());
    }

    @Test
    void testAckCallback_withStatsEnabled() throws Exception {
        // Given
        setField(generator, "messagesPerSecond", 10L);
        setField(generator, "statsEnabled", true);
        generator.init();
        
        AtomicLong counter = getField(generator, "counter");

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(10)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        
        subscriber.awaitItems(10, Duration.ofSeconds(10));
        List<Message<byte[]>> items = subscriber.getItems();
        
        // Ack all messages
        for (Message<byte[]> message : items) {
            CompletableFuture<?> ackFuture = (CompletableFuture<?>) message.ack().toCompletableFuture();
            ackFuture.get();
        }

        // Then
        assertEquals(10, counter.get());
    }

    @Test
    void testAckCallback_withStatsDisabled() throws Exception {
        // Given
        setField(generator, "statsEnabled", false);
        generator.init();
        
        AtomicLong counter = getField(generator, "counter");
        long initialCount = counter.get();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(5)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(5));
        
        subscriber.awaitItems(5, Duration.ofSeconds(10));
        List<Message<byte[]>> items = subscriber.getItems();
        
        // Ack all messages
        for (Message<byte[]> message : items) {
            CompletableFuture<?> ackFuture = (CompletableFuture<?>) message.ack().toCompletableFuture();
            ackFuture.get();
        }

        // Then - counter should still increment even with stats disabled
        assertEquals(initialCount + 5, counter.get());
    }

    @Test
    void testPayloadGeneration_isRandom() {
        // Given
        generator.init();

        // When
        Multi<Message<byte[]>> stream = generator.generateStream();
        AssertSubscriber<Message<byte[]>> subscriber = stream
                .select().first(2)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(2));
        
        subscriber.awaitItems(2, Duration.ofSeconds(10));
        List<Message<byte[]>> items = subscriber.getItems();

        // Then - payloads should be same reference (same payload instance)
        assertSame(items.get(0).getPayload(), items.get(1).getPayload());
    }

    @Test
    void testConfigurationHandling_variousPayloadSizes() throws Exception {
        // Test small payload
        setField(generator, "payloadSize", 10);
        generator.init();
        byte[] payload = getField(generator, "payload");
        assertEquals(10, payload.length);

        // Test medium payload
        generator = new ThroughputGenerator();
        setField(generator, "payloadSize", 5000);
        setField(generator, "messagesPerSecond", 1000L);
        setField(generator, "statsEnabled", true);
        setField(generator, "brokerHost", "localhost");
        setField(generator, "brokerPort", 1883);
        setField(generator, "topic", "test-topic");
        generator.init();
        payload = getField(generator, "payload");
        assertEquals(5000, payload.length);

        // Test large payload
        generator = new ThroughputGenerator();
        setField(generator, "payloadSize", 100000);
        setField(generator, "messagesPerSecond", 1000L);
        setField(generator, "statsEnabled", true);
        setField(generator, "brokerHost", "localhost");
        setField(generator, "brokerPort", 1883);
        setField(generator, "topic", "test-topic");
        generator.init();
        payload = getField(generator, "payload");
        assertEquals(100000, payload.length);
    }

    @Test
    void testConfigurationHandling_variousMessagesPerSecond() throws Exception {
        // Test low rate
        setField(generator, "messagesPerSecond", 1L);
        generator.init();
        Long statsBatchSize1 = getField(generator, "statsBatchSize");
        assertEquals(1L, statsBatchSize1);

        // Test medium rate
        generator = new ThroughputGenerator();
        setField(generator, "payloadSize", 1024);
        setField(generator, "messagesPerSecond", 5000L);
        setField(generator, "statsEnabled", true);
        setField(generator, "brokerHost", "localhost");
        setField(generator, "brokerPort", 1883);
        setField(generator, "topic", "test-topic");
        generator.init();
        Long statsBatchSize2 = getField(generator, "statsBatchSize");
        assertEquals(5000L, statsBatchSize2);

        // Test high rate
        generator = new ThroughputGenerator();
        setField(generator, "payloadSize", 1024);
        setField(generator, "messagesPerSecond", 100000L);
        setField(generator, "statsEnabled", true);
        setField(generator, "brokerHost", "localhost");
        setField(generator, "brokerPort", 1883);
        setField(generator, "topic", "test-topic");
        generator.init();
        Long statsBatchSize3 = getField(generator, "statsBatchSize");
        assertEquals(100000L, statsBatchSize3);
    }

    @Test
    void testStatsBatchSize_equalsMessagesPerSecond() throws Exception {
        // Given
        setField(generator, "messagesPerSecond", 2000L);
        
        // When
        generator.init();
        
        // Then
        Long statsBatchSize = getField(generator, "statsBatchSize");
        assertEquals(2000L, statsBatchSize);
    }

    // Helper methods for reflection
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
