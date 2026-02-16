package com.platform.application.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Detects duplicate Kafka events on the consumer side.
 * Uses Redis to track processed event IDs with a sliding window TTL.
 *
 * Interview Talking Point:
 * - Kafka guarantees at-least-once delivery, NOT exactly-once
 * - Consumer may receive the same event twice (rebalance, retry, etc.)
 * - This detector ensures idempotent processing on the consumer side
 * - event_id must be unique per event (UUID or composite key)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateEventDetector {

    private static final String KEY_PREFIX = "event:processed:";
    private static final Duration DEDUP_WINDOW = Duration.ofHours(48);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if an event has already been processed.
     * Returns true if this is a NEW event (not a duplicate).
     * Returns false if we've already processed this event.
     */
    public boolean isNewEvent(String eventId) {
        String redisKey = KEY_PREFIX + eventId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1",
                Objects.requireNonNull(DEDUP_WINDOW, "DEDUP_WINDOW must not be null"));
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("Duplicate event detected: {}", eventId);
        }
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Mark an event as processed.
     * Called after successful processing.
     */
    public void markProcessed(String eventId) {
        String redisKey = KEY_PREFIX + eventId;
        Duration ttl = Objects.requireNonNull(DEDUP_WINDOW, "DEDUP_WINDOW must not be null");
        redisTemplate.opsForValue().set(redisKey, "processed", ttl);
    }
}
