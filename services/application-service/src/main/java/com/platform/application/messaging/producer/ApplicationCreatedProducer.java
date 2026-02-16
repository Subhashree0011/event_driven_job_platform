package com.platform.application.messaging.producer;

import com.platform.application.dto.ApplicationEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Direct Kafka producer for application events.
 * Note: The primary event publishing path is via the Transactional Outbox Pattern.
 * This producer is used for non-critical events that don't need transactional guarantees.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationCreatedProducer {

    private static final String TOPIC = "application.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Directly publish an application event (for non-critical events).
     * Partition key = jobId to ensure ordering per job.
     */
    public void publish(ApplicationEvent event) {
        String key = Objects.requireNonNull(
                String.valueOf(event.getJobId()), "Kafka partition key must not be null");

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish APPLICATION_CREATED for application {}: {}",
                        event.getApplicationId(), ex.getMessage());
                meterRegistry.counter("kafka.publish.error",
                        "topic", TOPIC).increment();
            } else {
                log.debug("Published APPLICATION_CREATED: application={}, partition={}, offset={}",
                        event.getApplicationId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                meterRegistry.counter("kafka.publish.success",
                        "topic", TOPIC).increment();
            }
        });
    }
}
