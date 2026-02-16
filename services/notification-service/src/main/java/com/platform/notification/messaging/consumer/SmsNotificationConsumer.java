package com.platform.notification.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.platform.notification.idempotency.NotificationDeduplicator;
import com.platform.notification.metrics.NotificationLatencyMetrics;
import com.platform.notification.retry.RetryHandler;
import com.platform.notification.service.SmsService;
import com.platform.notification.util.NotificationFormatter;

import java.util.Map;

/**
 * Kafka consumer for SMS notifications.
 * 
 * Consumes from: application.created (same topic, different consumer group behavior)
 * Consumer group: notification-sms-group (separate from email to allow independent scaling)
 * 
 * Only triggers SMS for high-priority events:
 *   - APPLICATION_STATUS_CHANGED with status OFFERED or INTERVIEW
 *   - Not all events warrant an SMS (cost + user experience)
 * 
 * Why separate consumer group from email:
 *   - SMS has different rate limits
 *   - Email backlog shouldn't block urgent SMS
 *   - Independent scaling and monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationConsumer {

    private final SmsService smsService;
    private final NotificationDeduplicator deduplicator;
    private final RetryHandler retryHandler;
    private final NotificationLatencyMetrics latencyMetrics;
    private final NotificationFormatter formatter;

    @KafkaListener(
            topics = "application.created",
            groupId = "notification-sms-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationEvent(ConsumerRecord<String, Map<String, Object>> record) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> event = record.value();

        if (event == null) {
            log.warn("Null event value (tombstone?) at partition={}, offset={}", record.partition(), record.offset());
            return;
        }

        String eventType = String.valueOf(event.getOrDefault("eventType", "UNKNOWN"));
        String status = String.valueOf(event.getOrDefault("status", ""));

        // Only send SMS for high-priority status changes
        if (!shouldSendSms(eventType, status)) {
            return;
        }

        String eventId = "sms:" + eventType + ":" + event.getOrDefault("applicationId", record.offset());

        if (!deduplicator.shouldProcess(eventId)) {
            return;
        }

        try {
            Long userId = toLong(event.get("userId"));
            String phoneNumber = formatter.resolveUserPhone(userId);

            if (phoneNumber == null || phoneNumber.isBlank()) {
                log.debug("No phone number for user {}, skipping SMS", userId);
                return;
            }

            String message = formatter.formatSmsMessage(eventType, status, event);
            smsService.sendSms(phoneNumber, message);

            long duration = System.currentTimeMillis() - startTime;
            latencyMetrics.recordProcessingLatency("sms", eventType, duration);

        } catch (Exception e) {
            log.error("SMS notification failed: eventId={}", eventId, e);
            deduplicator.releaseForRetry(eventId);

            String userId = String.valueOf(event.getOrDefault("userId", "unknown"));
            retryHandler.scheduleRetry(event, "SMS", 0, userId, e);
        }
    }

    /**
     * Filter: Only send SMS for critical status changes.
     * SMS costs money — don't send for every event.
     */
    private boolean shouldSendSms(String eventType, String status) {
        if (!"APPLICATION_STATUS_CHANGED".equals(eventType)) {
            return false;
        }
        return "OFFERED".equals(status) || "INTERVIEW".equals(status);
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
