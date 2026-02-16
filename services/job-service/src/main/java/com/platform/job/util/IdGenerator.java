package com.platform.job.util;

import java.util.UUID;

/**
 * Generates unique identifiers for events, correlation IDs, etc.
 */
public final class IdGenerator {

    private IdGenerator() {
        // Utility class
    }

    /**
     * Generate a UUID-based unique ID.
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a correlation ID for tracing requests across services.
     */
    public static String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
