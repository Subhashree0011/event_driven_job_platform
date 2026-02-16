package com.platform.application.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting using sliding window counter.
 *
 * Design: Uses Redis INCR + EXPIRE for atomic rate limiting.
 * Pattern mirrors user-service implementation for consistency.
 *
 * Used for:
 * - Application submission per user (production mode)
 * - API rate limiting per IP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:app:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Check if the action is within rate limit.
     * Uses Redis INCR + EXPIRE for atomic counting.
     *
     * @param key           Unique key (e.g., "apply:userId:123")
     * @param limit         Max allowed requests
     * @param windowSeconds Time window in seconds
     * @return true if within limit, false if exceeded
     */
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            return count != null && count <= limit;
        } catch (Exception e) {
            log.warn("Rate limit check failed for key '{}': {}", key, e.getMessage());
            return true; // Fail open — allow request if Redis is down
        }
    }

    /**
     * Get remaining requests for a key.
     */
    public long getRemainingRequests(String key, int limit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            String count = redisTemplate.opsForValue().get(redisKey);
            if (count == null) return limit;
            return Math.max(0, limit - Long.parseLong(count));
        } catch (Exception e) {
            return limit;
        }
    }

    /**
     * Get current request count for a key (used in metrics/testing).
     */
    public long getCurrentCount(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            String count = redisTemplate.opsForValue().get(redisKey);
            return count != null ? Long.parseLong(count) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
