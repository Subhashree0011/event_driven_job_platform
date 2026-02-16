package com.platform.application.outbox;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Publishes outbox events to Kafka.
 * Uses the event's partition_key (job_id) for ordering guarantees.
 *
 * Interview Talking Point:
 * - Synchronous send (with timeout) ensures we know if publish succeeded before marking as published
 * - If we used async send, a crash between send and markPublished could lose the event
 * - Circuit breaker on Kafka prevents cascading failures during broker outages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @CircuitBreaker(name = "kafkaCircuitBreaker")
    public void publish(OutboxEvent event) {
        String topic = Objects.requireNonNull(event.getTopic(), "Outbox event topic must not be null");
        String key = Objects.requireNonNull(event.getPartitionKey(), "Outbox event partition key must not be null");
        String payload = event.getPayload();

        try {
            // Synchronous send — wait for ack before marking as published
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, payload);

            SendResult<String, Object> result = future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.debug("Outbox event {} published to topic={}, partition={}, offset={}",
                    event.getId(), topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("Failed to publish outbox event {} to topic {}: {}",
                    event.getId(), topic, e.getMessage());
            meterRegistry.counter("outbox.kafka.error",
                    "topic", topic, "eventType", event.getEventType()).increment();
            throw new RuntimeException("Failed to publish outbox event to Kafka", e);
        }
    }
}
