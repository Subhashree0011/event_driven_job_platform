package com.platform.notification.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.platform.notification.idempotency.NotificationDeduplicator;
import com.platform.notification.metrics.NotificationLatencyMetrics;
import com.platform.notification.retry.RetryHandler;
import com.platform.notification.service.EmailService;
import com.platform.notification.util.NotificationFormatter;

import java.util.Map;

/**
 * Kafka consumer for email notifications.
 * 
 * Consumes from: application.created (12 partitions, keyed by job_id)
 * Consumer group: notification-service-group
 * 
 * Event types handled:
 *   - APPLICATION_CREATED → Send application confirmation email
 *   - APPLICATION_STATUS_CHANGED → Send status update email
 *   - APPLICATION_WITHDRAWN → Send withdrawal confirmation email
 * 
 * Design:
 *   - Idempotent: NotificationDeduplicator prevents duplicate sends
 *   - Resilient: Failed sends routed to notification.retry topic
 *   - Observable: Latency + failure metrics tracked
 * 
 * Why small poll batch (10):
 *   - SMTP is I/O-heavy (1-5s per email)
 *   - Large batches cause GC pressure + session timeout risks (Incident #2)
 *   - 10 records × 5s = 50s worst case, well within poll interval
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final EmailService emailService;
    private final NotificationDeduplicator deduplicator;
    private final RetryHandler retryHandler;
    private final NotificationLatencyMetrics latencyMetrics;
    private final NotificationFormatter formatter;

    @KafkaListener(
            topics = "application.created",
            groupId = "notification-email-group",
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
        String eventId = buildEventId(event, record);

        log.debug("Processing email notification: eventType={}, eventId={}", eventType, eventId);

        // Step 1: Deduplication check
        if (!deduplicator.shouldProcess(eventId)) {
            return; // Already processed — skip
        }

        try {
            // Step 2: Route by event type
            switch (eventType) {
                case "APPLICATION_CREATED" -> handleApplicationCreated(event);
                case "APPLICATION_STATUS_CHANGED" -> handleStatusChanged(event);
                case "APPLICATION_WITHDRAWN" -> handleWithdrawn(event);
                default -> log.warn("Unhandled event type for email: {}", eventType);
            }

            long duration = System.currentTimeMillis() - startTime;
            latencyMetrics.recordProcessingLatency("email", eventType, duration);

        } catch (Exception e) {
            log.error("Email notification failed: eventId={}, error={}", eventId, e.getMessage());
            deduplicator.releaseForRetry(eventId);

            // Route to retry topic with metadata
            String userId = String.valueOf(event.getOrDefault("userId", "unknown"));
            retryHandler.scheduleRetry(event, "EMAIL", 0, userId, e);
        }
    }

    private void handleApplicationCreated(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        Long jobId = toLong(event.get("jobId"));
        Long applicationId = toLong(event.get("applicationId"));

        // In production: fetch user email + job details from User/Job service via REST or cache
        String recipientEmail = formatter.resolveUserEmail(userId);
        String jobTitle = formatter.resolveJobTitle(jobId);

        Map<String, Object> templateVars = Map.of(
                "userName", "User " + userId,
                "jobTitle", jobTitle,
                "companyName", "Company",
                "applicationId", applicationId,
                "year", java.time.Year.now().getValue()
        );

        emailService.sendEmail(
                recipientEmail,
                "Application Confirmed: " + jobTitle,
                "application-confirmation",
                templateVars
        );
    }

    private void handleStatusChanged(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String status = String.valueOf(event.getOrDefault("status", "UNKNOWN"));

        String recipientEmail = formatter.resolveUserEmail(userId);

        Map<String, Object> templateVars = Map.of(
                "userName", "User " + userId,
                "status", status,
                "year", java.time.Year.now().getValue()
        );

        emailService.sendEmail(
                recipientEmail,
                "Application Status Update: " + status,
                "status-update",
                templateVars
        );
    }

    private void handleWithdrawn(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));

        String recipientEmail = formatter.resolveUserEmail(userId);

        emailService.sendSimpleEmail(
                recipientEmail,
                "Application Withdrawn",
                "Your job application has been successfully withdrawn."
        );
    }

    /**
     * Build a unique event ID for deduplication.
     * Combines event fields to ensure uniqueness across retries.
     */
    private String buildEventId(Map<String, Object> event, ConsumerRecord<String, ?> record) {
        Object appId = event.get("applicationId");
        Object eventType = event.get("eventType");
        if (appId != null && eventType != null) {
            return "email:" + eventType + ":" + appId;
        }
        // Fallback to Kafka coordinates (topic-partition-offset)
        return "email:" + record.topic() + "-" + record.partition() + "-" + record.offset();
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
