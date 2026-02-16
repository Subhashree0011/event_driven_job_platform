package com.platform.application.messaging.consumer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes retry events from the notification retry topic.
 * Events that failed initial processing are republished to this topic
 * with incremented retry counts and exponential backoff.
 *
 * Interview Talking Point:
 * - Retry topic pattern separates retry logic from main processing
 * - Exponential backoff prevents overwhelming downstream services
 * - After max retries, events go to dead letter topic for manual inspection
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryConsumer {

    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "notification.retry",
            groupId = "application-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRetryEvent(Map<String, Object> event) {
        String eventId = (String) event.getOrDefault("eventId", "");
        Integer retryCount = (Integer) event.getOrDefault("retryCount", 0);

        log.info("Processing retry event: id={}, retryCount={}", eventId, retryCount);

        try {
            // Process the retried event
            meterRegistry.counter("kafka.retry.processed",
                    "topic", "notification.retry").increment();

        } catch (Exception e) {
            log.error("Retry event processing failed: id={}, error={}", eventId, e.getMessage());
            meterRegistry.counter("kafka.retry.failed",
                    "topic", "notification.retry").increment();
        }
    }
}
