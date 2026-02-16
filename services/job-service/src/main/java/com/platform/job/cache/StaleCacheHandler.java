package com.platform.job.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Handles stale cache serving when the primary data source is unavailable.
 *
 * Interview Talking Point:
 * - When DB circuit breaker is open, we can serve stale cached data
 * - Better to show slightly outdated results than an error page
 * - Stale entries have a separate "shadow" TTL longer than the primary TTL
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaleCacheHandler {

    private static final String STALE_PREFIX = "stale:";
    private static final Duration STALE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Store a shadow copy of the cached value with a longer TTL.
     * This shadow copy is served when the primary cache expires and the DB is down.
     */
    public void storeStaleCopy(String cacheKey, Object value) {
        if (value == null) return;
        try {
            redisTemplate.opsForValue().set(STALE_PREFIX + cacheKey, value,
                    Objects.requireNonNull(STALE_TTL, "STALE_TTL must not be null"));
        } catch (Exception e) {
            log.warn("Failed to store stale cache copy for key {}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Retrieve the stale (shadow) copy when primary cache and DB are both unavailable.
     */
    public Object getStaleCopy(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(STALE_PREFIX + cacheKey);
        } catch (Exception e) {
            log.warn("Failed to retrieve stale cache for key {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }
}
