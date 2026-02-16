package com.platform.notification.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.platform.notification.config.RetryConfig;
import com.platform.notification.metrics.RetryMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles retry routing for failed notification deliveries.
 * 
 * Strategy:
 *   - Failed notifications are published to "notification.retry" topic
 *   - Retry metadata (attempt count, next delay) embedded in the event
 *   - RetryConsumer picks up and re-attempts delivery after the delay
 *   - After max attempts → dead letter (logged + metric, no more retries)
 * 
 * Why Kafka-based retry over in-memory:
 *   - Survives service restarts
 *   - Distributed across instances
 *   - Observable (consumer lag = retry backlog)
 *   - Audit trail via Kafka retention
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RetryConfig retryConfig;
    private final RetryMetrics retryMetrics;

    private static final String RETRY_TOPIC = "notification.retry";

    /**
     * Route a failed notification to the retry topic.
     * 
     * @param event Original event payload
     * @param channel Delivery channel (EMAIL, SMS, PUSH)
     * @param attempt Current attempt number
     * @param userId User ID for partition key (ordering per user)
     * @param cause The failure reason
     */
    public void scheduleRetry(Map<String, Object> event, String channel,
                               int attempt, String userId, Throwable cause) {

        int maxAttempts = retryConfig.getRetry().getMaxAttempts();

        if (attempt >= maxAttempts) {
            log.error("Max retry attempts ({}) exhausted for channel={}, userId={}, cause={}",
                    maxAttempts, channel, userId, cause.getMessage());
            retryMetrics.recordDeadLetter(channel);
            return; // Dead letter — no more retries
        }

        long delay = retryConfig.getRetry().calculateDelay(attempt + 1);

        Map<String, Object> retryEvent = new HashMap<>(event);
        retryEvent.put("_retry_attempt", attempt + 1);
        retryEvent.put("_retry_channel", channel);
        retryEvent.put("_retry_delay_ms", delay);
        retryEvent.put("_retry_reason", cause.getMessage());
        retryEvent.put("_retry_scheduled_at", System.currentTimeMillis());

        kafkaTemplate.send(RETRY_TOPIC,
                Objects.requireNonNull(userId, "userId partition key must not be null"), retryEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish retry event for userId={}: {}",
                                userId, ex.getMessage());
                        retryMetrics.recordRetryPublishFailure(channel);
                    } else {
                        log.info("Retry scheduled: channel={}, attempt={}/{}, delay={}ms, userId={}",
                                channel, attempt + 1, maxAttempts, delay, userId);
                        retryMetrics.recordRetryScheduled(channel, attempt + 1);
                    }
                });
    }

    /**
     * Check if an event has exhausted its retries.
     */
    public boolean isExhausted(int attempt) {
        return attempt >= retryConfig.getRetry().getMaxAttempts();
    }
}
