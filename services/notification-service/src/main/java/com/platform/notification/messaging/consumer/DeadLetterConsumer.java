package com.platform.notification.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.platform.notification.metrics.RetryMetrics;
import com.platform.notification.retry.BackoffPolicy;
import com.platform.notification.retry.RetryTracker;
import com.platform.notification.service.EmailService;
import com.platform.notification.service.SmsService;
import com.platform.notification.util.NotificationFormatter;

import java.util.Map;

/**
 * Dead letter / retry consumer for failed notifications.
 * 
 * Consumes from: notification.retry (3 partitions, keyed by user_id)
 * 
 * Uses retryListenerContainerFactory with single-thread concurrency
 * to prevent retry storm amplification.
 * 
 * Flow:
 *   1. Read retry event from topic
 *   2. Check attempt count against max
 *   3. If within limit: sleep for backoff delay, then re-attempt delivery
 *   4. If exhausted: log as dead letter, increment metric, move on
 * 
 * Why Kafka for retries instead of in-memory scheduler:
 *   - Survives pod restarts
 *   - Visible as consumer lag (monitoring)
 *   - Ordered per user (partition key = user_id)
 *   - Audit trail via Kafka retention
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

    private final EmailService emailService;
    private final SmsService smsService;
    private final BackoffPolicy backoffPolicy;
    private final RetryTracker retryTracker;
    private final RetryMetrics retryMetrics;
    private final NotificationFormatter formatter;

    @KafkaListener(
            topics = "notification.retry",
            groupId = "notification-retry-group",
            containerFactory = "retryListenerContainerFactory"
    )
    public void onRetryEvent(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> event = record.value();

        if (event == null) {
            log.warn("Null retry event at partition={}, offset={}", record.partition(), record.offset());
            return;
        }

        String channel = String.valueOf(event.getOrDefault("_retry_channel", "UNKNOWN"));
        int attempt = toInt(event.getOrDefault("_retry_attempt", 1));
        long delayMs = toLong(event.getOrDefault("_retry_delay_ms", 1000L));
        String eventId = "retry:" + channel + ":" + record.partition() + "-" + record.offset();

        log.info("Processing retry: channel={}, attempt={}, delay={}ms", channel, attempt, delayMs);

        // Check if exhausted
        if (!backoffPolicy.canRetry(attempt)) {
            log.error("DEAD LETTER: channel={}, eventId={} — all retries exhausted", channel, eventId);
            retryTracker.markExhausted(eventId);
            retryMetrics.recordDeadLetter(channel);
            return;
        }

        // Apply backoff delay
        try {
            long jitteredDelay = backoffPolicy.calculateDelay(attempt);
            log.debug("Backoff delay: {}ms (attempt {})", jitteredDelay, attempt);
            Thread.sleep(jitteredDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Re-attempt delivery
        try {
            switch (channel) {
                case "EMAIL" -> retryEmailDelivery(event);
                case "SMS" -> retrySmsDelivery(event);
                default -> log.warn("Unknown retry channel: {}", channel);
            }

            retryTracker.clearRetryState(eventId);
            retryMetrics.recordRetrySuccess(channel, attempt);
            log.info("Retry succeeded: channel={}, attempt={}", channel, attempt);

        } catch (Exception e) {
            log.error("Retry failed: channel={}, attempt={}", channel, attempt, e);
            retryMetrics.recordRetryFailure(channel, attempt);
            // Event will not be re-published — it's already in the retry topic
            // The original RetryHandler controls whether to schedule another retry
        }
    }

    private void retryEmailDelivery(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String recipientEmail = formatter.resolveUserEmail(userId);
        String eventType = String.valueOf(event.getOrDefault("eventType", "notification"));

        emailService.sendSimpleEmail(
                recipientEmail,
                "Notification: " + eventType,
                formatter.formatFallbackText(event)
        );
    }

    private void retrySmsDelivery(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String phoneNumber = formatter.resolveUserPhone(userId);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String eventType = String.valueOf(event.getOrDefault("eventType", "notification"));
            String status = String.valueOf(event.getOrDefault("status", ""));
            smsService.sendSms(phoneNumber, formatter.formatSmsMessage(eventType, status, event));
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
        }
        return 0L;
    }
}
