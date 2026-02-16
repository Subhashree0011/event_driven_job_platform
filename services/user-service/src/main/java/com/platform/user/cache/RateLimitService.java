package com.platform.user.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting using sliding window counter.
 * 
 * Design: Uses Redis INCR + EXPIRE for atomic rate limiting.
 * This is simpler and more performant than database-based rate limiting.
 * 
 * Used for:
 * - Login attempts per IP/email
 * - API rate limiting per user
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Check if the action is within rate limit.
     * Uses Redis INCR + EXPIRE for atomic counting.
     * 
     * @param key     Unique key (e.g., "login:192.168.1.1" or "api:user123")
     * @param limit   Max allowed requests
     * @param windowSeconds Time window in seconds
     * @return true if within limit, false if exceeded
     */
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                // First request — set expiration
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            return count != null && count <= limit;
        } catch (Exception e) {
            log.warn("Rate limit check failed: {}", e.getMessage());
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
}
