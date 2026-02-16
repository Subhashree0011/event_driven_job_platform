package com.platform.notification.retry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Tracks retry state for individual notifications using Redis.
 * 
 * Key pattern: notification:retry:{eventId}
 * Value: current attempt count (string integer)
 * TTL: 1 hour (after which we assume the event is stale)
 * 
 * Why Redis for retry tracking:
 *   - Notification service is stateless (no DB)
 *   - Survives pod restarts (unlike in-memory counters)
 *   - Atomic increment via INCR
 *   - Auto-cleanup via TTL
 */
@Component
@Slf4j
public class RetryTracker {

    private final StringRedisTemplate redisTemplate;
    private final Counter retryExhaustedCounter;

    private static final String KEY_PREFIX = "notification:retry:";
    private static final Duration RETRY_STATE_TTL = Duration.ofHours(1);

    public RetryTracker(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.retryExhaustedCounter = Counter.builder("notification.retries.exhausted")
                .description("Number of notifications that exhausted all retry attempts")
                .register(meterRegistry);
    }

    /**
     * Get the current retry attempt for an event.
     * 
     * @param eventId Unique event identifier
     * @return Current attempt number (0 if first attempt)
     */
    public int getCurrentAttempt(String eventId) {
        String key = KEY_PREFIX + eventId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Non-numeric retry count for '{}': '{}'", eventId, value);
            return 0;
        }
    }

    /**
     * Increment and return the retry attempt count.
     * Sets TTL on first increment.
     * 
     * @param eventId Unique event identifier
     * @return New attempt count after increment
     */
    public int incrementAttempt(String eventId) {
        String key = KEY_PREFIX + eventId;
        Long newCount = redisTemplate.opsForValue().increment(key);

        // Set TTL only on first increment (when count becomes 1)
        if (newCount != null && newCount == 1) {
            Duration ttl = Objects.requireNonNull(RETRY_STATE_TTL, "RETRY_STATE_TTL must not be null");
            redisTemplate.expire(key, ttl);
        }

        int attempt = newCount != null ? newCount.intValue() : 1;
        log.debug("Retry attempt incremented: eventId={}, attempt={}", eventId, attempt);
        return attempt;
    }

    /**
     * Clear retry state after successful delivery.
     */
    public void clearRetryState(String eventId) {
        String key = KEY_PREFIX + eventId;
        redisTemplate.delete(key);
        log.debug("Retry state cleared: eventId={}", eventId);
    }

    /**
     * Mark an event as exhausted (all retries failed).
     */
    public void markExhausted(String eventId) {
        clearRetryState(eventId);
        retryExhaustedCounter.increment();
        log.error("All retry attempts exhausted for event: {}", eventId);
    }
}
