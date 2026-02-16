package com.platform.application.util;

import java.util.UUID;

/**
 * Generates unique event IDs for outbox events and correlation tracking.
 */
public final class EventIdGenerator {

    private EventIdGenerator() {
        // Utility class
    }

    /**
     * Generate a unique event ID.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate an idempotency key from userId and jobId.
     * This ensures the same user applying to the same job produces the same key.
     */
    public static String generateIdempotencyKey(Long userId, Long jobId) {
        return "apply:" + userId + ":" + jobId;
    }

    /**
     * Generate a correlation ID for tracing events across services.
     */
    public static String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
