package com.platform.user.cache;

import com.platform.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Write-through cache for user profiles.
 * 
 * Pattern: Write-Through
 * - On read: check cache → miss → load from DB → write to cache
 * - On write: update DB → update cache immediately
 * 
 * TTL: 30 minutes (user profiles don't change frequently)
 * 
 * Why not Cache-Aside for profiles?
 * Users expect immediate reflection of their changes.
 * Write-through ensures the cache is always consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final String USER_CACHE_PREFIX = "user:profile:";
    private static final long USER_CACHE_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get cached user profile.
     */
    public UserResponse getCachedUser(Long userId) {
        try {
            String key = USER_CACHE_PREFIX + userId;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof UserResponse userResponse) {
                return userResponse;
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis read failed for user {}: {}", userId, e.getMessage());
            return null; // Graceful degradation — fall through to DB
        }
    }

    /**
     * Write user profile to cache (write-through).
     */
    public void cacheUser(Long userId, UserResponse response) {
        if (response == null) return;
        try {
            String key = USER_CACHE_PREFIX + userId;
            redisTemplate.opsForValue().set(key, response, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for user {}: {}", userId, e.getMessage());
            // Don't fail the request — cache is optional
        }
    }

    /**
     * Invalidate user cache entry.
     */
    public void evictUser(Long userId) {
        try {
            String key = USER_CACHE_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis delete failed for user {}: {}", userId, e.getMessage());
        }
    }
}
