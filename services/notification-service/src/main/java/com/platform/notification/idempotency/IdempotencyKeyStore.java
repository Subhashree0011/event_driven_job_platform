package com.platform.notification.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-backed idempotency key store for notification deduplication.
 * 
 * Uses Redis SETNX (SET if Not eXists) for atomic check-and-set.
 * 
 * Key pattern: notification:idem:{eventId}
 * TTL: 24 hours (configurable) — MUST be longer than max consumer lag.
 * 
 * Lesson from Incident #4 (Duplicate Notifications):
 *   Previously used 60s TTL. When consumer lag exceeded 60s, the key expired
 *   before the message was reprocessed, causing duplicate emails.
 *   Now uses 24h TTL to survive worst-case lag scenarios.
 * 
 * Why Redis over DB:
 *   - Notification service is stateless (no DB)
 *   - Redis SETNX is atomic and fast
 *   - 24h TTL auto-cleans expired keys (no cleanup job needed)
 *   - Acceptable trade-off: Redis restart = potential duplicates (at-least-once)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyStore {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "notification:idem:";

    /**
     * Try to acquire an idempotency key.
     * 
     * @param eventId Unique event identifier (e.g., from Kafka message)
     * @param ttlSeconds How long to keep the key (default: 86400 = 24h)
     * @return true if this is the FIRST time we've seen this event (proceed with sending)
     *         false if this event was already processed (skip — duplicate)
     */
    public boolean tryAcquire(String eventId, long ttlSeconds) {
        String key = KEY_PREFIX + eventId;
        Duration ttl = Objects.requireNonNull(Duration.ofSeconds(ttlSeconds));
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl);

        if (Boolean.TRUE.equals(isNew)) {
            log.debug("Idempotency key acquired: {}", eventId);
            return true;
        }

        log.info("Duplicate event detected, skipping: {}", eventId);
        return false;
    }

    /**
     * Check if an event has already been processed without acquiring the key.
     */
    public boolean isProcessed(String eventId) {
        String key = KEY_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Release an idempotency key (e.g., if processing fails and we want retry).
     * Use with caution — only release if the notification was NOT sent.
     */
    public void release(String eventId) {
        String key = KEY_PREFIX + eventId;
        redisTemplate.delete(key);
        log.debug("Idempotency key released: {}", eventId);
    }
}
