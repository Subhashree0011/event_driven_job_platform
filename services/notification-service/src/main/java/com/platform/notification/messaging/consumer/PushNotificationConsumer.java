package com.platform.notification.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.platform.notification.idempotency.NotificationDeduplicator;
import com.platform.notification.metrics.NotificationLatencyMetrics;
import com.platform.notification.service.PushService;
import com.platform.notification.util.NotificationFormatter;

import java.util.Map;

/**
 * Kafka consumer for push notifications.
 * 
 * Consumes from: application.created
 * Consumer group: notification-push-group
 * 
 * Push notifications are sent for all application events
 * (unlike SMS which is selective). Push is free and non-intrusive.
 * 
 * Design:
 *   - Separate consumer group for independent scaling
 *   - No retry routing — push failures are fire-and-forget
 *   - FCM/APNs handle their own retry internally
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationConsumer {

    private final PushService pushService;
    private final NotificationDeduplicator deduplicator;
    private final NotificationLatencyMetrics latencyMetrics;
    private final NotificationFormatter formatter;

    @KafkaListener(
            topics = "application.created",
            groupId = "notification-push-group",
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
        String eventId = "push:" + eventType + ":" + event.getOrDefault("applicationId", record.offset());

        if (!deduplicator.shouldProcess(eventId)) {
            return;
        }

        try {
            Long userId = toLong(event.get("userId"));
            String title = formatter.formatPushTitle(eventType);
            String body = formatter.formatPushBody(eventType, event);

            pushService.sendPush(String.valueOf(userId), title, body);

            long duration = System.currentTimeMillis() - startTime;
            latencyMetrics.recordProcessingLatency("push", eventType, duration);

        } catch (Exception e) {
            // Push failures are non-critical — log and move on
            log.warn("Push notification failed (non-critical): eventId={}, error={}",
                    eventId, e.getMessage());
        }
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
