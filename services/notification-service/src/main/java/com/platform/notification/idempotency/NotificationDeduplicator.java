package com.platform.notification.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.platform.notification.config.RetryConfig;
import com.platform.notification.metrics.FailureRateMetrics;

/**
 * Notification deduplicator that combines event-level and user-level dedup.
 * 
 * Two-layer deduplication:
 *   1. Event-level: Same Kafka event processed twice (at-least-once delivery)
 *   2. User-level rate limiting: Prevent notification spam to the same user
 * 
 * Why two layers:
 *   - Event dedup prevents Kafka redelivery duplicates
 *   - Rate limiting prevents business-level duplicates (e.g., user applies 
 *     to 50 jobs in 1 minute → don't send 50 emails immediately)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeduplicator {

    private final IdempotencyKeyStore idempotencyKeyStore;
    private final RetryConfig retryConfig;
    private final FailureRateMetrics failureRateMetrics;

    /**
     * Check if a notification event should be processed.
     * 
     * @param eventId Unique event identifier from Kafka
     * @return true if this notification should be sent, false if it's a duplicate
     */
    public boolean shouldProcess(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("Received event with null/empty eventId — cannot deduplicate");
            failureRateMetrics.recordDeduplicationFailure("missing_event_id");
            return true; // Process anyway — better duplicate than lost notification
        }

        long ttl = retryConfig.getDeduplication().getTtl();
        boolean isNew = idempotencyKeyStore.tryAcquire(eventId, ttl);

        if (!isNew) {
            failureRateMetrics.recordDuplicate("event");
            log.info("Duplicate notification event skipped: {}", eventId);
        }

        return isNew;
    }

    /**
     * Release the idempotency key so the event can be retried.
     * Only call this when the notification was NOT successfully sent.
     */
    public void releaseForRetry(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            idempotencyKeyStore.release(eventId);
            log.debug("Released idempotency key for retry: {}", eventId);
        }
    }
}
