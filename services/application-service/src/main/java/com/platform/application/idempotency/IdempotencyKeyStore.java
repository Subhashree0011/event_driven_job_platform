package com.platform.application.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-based idempotency key store.
 * Prevents processing the same request twice (e.g., double-click submit).
 *
 * Interview Talking Point:
 * - Client sends an idempotency key (UUID) in the X-Idempotency-Key header
 * - First request: store key in Redis with TTL, process normally
 * - Second request with same key: return cached response without re-processing
 * - TTL of 24 hours balances safety vs memory usage
 * - Redis SETNX is atomic — no race condition between check and set
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyStore {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration KEY_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Try to acquire an idempotency key.
     * Returns true if this is the first time we've seen this key.
     * Returns false if the key already exists (duplicate request).
     */
    public boolean tryAcquire(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "processing",
                Objects.requireNonNull(KEY_TTL, "KEY_TTL must not be null"));
        return Boolean.TRUE.equals(isNew);
    }

    /**
     * Store the response for an idempotency key.
     * If the client retries, we return this cached response.
     */
    public void storeResponse(String idempotencyKey, Object response) {
        if (response == null) return;
        String redisKey = KEY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(redisKey, response,
                Objects.requireNonNull(KEY_TTL, "KEY_TTL must not be null"));
    }

    /**
     * Get the cached response for an idempotency key.
     * Returns null if no response is cached.
     */
    public Object getResponse(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        return redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Remove an idempotency key (e.g., if processing failed and we want to allow retry).
     */
    public void release(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(redisKey);
    }
}
