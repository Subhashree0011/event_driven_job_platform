package com.platform.job.messaging.producer;

import com.platform.job.dto.JobEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes job lifecycle events to the "job.lifecycle" Kafka topic.
 * Uses the jobId as partition key to ensure ordering per job.
 *
 * Interview Talking Point:
 * - Partition key = jobId guarantees event ordering for the same job
 * - CompletableFuture for async send with callback logging
 * - Idempotent producer (enable.idempotence=true) prevents duplicates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventProducer {

    private static final String TOPIC = "job.lifecycle";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public void publishJobCreated(JobEvent event) {
        sendEvent(event, "JOB_CREATED");
    }

    public void publishJobUpdated(JobEvent event) {
        sendEvent(event, "JOB_UPDATED");
    }

    public void publishJobStatusChanged(JobEvent event) {
        sendEvent(event, "JOB_STATUS_CHANGED");
    }

    private void sendEvent(JobEvent event, String eventType) {
        String key = Objects.requireNonNull(
                String.valueOf(event.getJobId()), "Kafka partition key must not be null");

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} event for job {}: {}",
                        eventType, event.getJobId(), ex.getMessage());
                meterRegistry.counter("kafka.publish.error",
                        "topic", TOPIC, "eventType", eventType).increment();
            } else {
                log.debug("Published {} event for job {} to partition {} offset {}",
                        eventType, event.getJobId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                meterRegistry.counter("kafka.publish.success",
                        "topic", TOPIC, "eventType", eventType).increment();
            }
        });
    }
}
