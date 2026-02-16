package com.platform.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized retry configuration for notification delivery.
 * 
 * Maps to application.yml:
 *   notification.retry.max-attempts: 3
 *   notification.retry.initial-interval: 1000
 *   notification.retry.multiplier: 2.0
 *   notification.retry.max-interval: 30000
 *   notification.deduplication.ttl: 86400
 * 
 * Why externalized:
 *   - Different environments need different retry aggressiveness
 *   - Prod may tolerate more retries; staging may want fast failure
 *   - Dedup TTL must survive worst-case consumer lag (Incident #4 lesson)
 */
@Configuration
@ConfigurationProperties(prefix = "notification")
@Getter
@Setter
public class RetryConfig {

    private RetryProperties retry = new RetryProperties();
    private DeduplicationProperties deduplication = new DeduplicationProperties();

    @Getter
    @Setter
    public static class RetryProperties {
        private int maxAttempts = 3;
        private long initialInterval = 1000L;
        private double multiplier = 2.0;
        private long maxInterval = 30000L;

        /**
         * Calculate backoff delay for a given attempt number.
         * Uses exponential backoff with a cap.
         * 
         * Example with defaults:
         *   Attempt 1: 1000ms
         *   Attempt 2: 2000ms
         *   Attempt 3: 4000ms (capped at 30000ms)
         */
        public long calculateDelay(int attempt) {
            long delay = (long) (initialInterval * Math.pow(multiplier, attempt - 1));
            return Math.min(delay, maxInterval);
        }
    }

    @Getter
    @Setter
    public static class DeduplicationProperties {
        /**
         * TTL in seconds for idempotency keys in Redis.
         * MUST be longer than max expected consumer lag.
         * 
         * Lesson from Incident #4: Setting this to 60s caused duplicates
         * when consumer lag exceeded 60s. Now set to 24h (86400s).
         */
        private long ttl = 86400L;
    }
}
