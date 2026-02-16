package com.platform.job.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Distributed lock using Redis SETNX to prevent cache stampede.
 *
 * Interview Talking Point:
 * - When a hot cache key expires, multiple threads might try to rebuild it simultaneously
 * - Lock ensures only one thread rebuilds the cache; others wait or serve stale data
 * - Lock has a TTL to prevent deadlocks if the lock holder crashes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheLockManager {

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Try to acquire a distributed lock for cache rebuild.
     * Uses Redis SETNX (SET if Not eXists) — atomic operation.
     *
     * @return true if lock acquired, false if another thread holds it
     */
    public boolean tryLock(String cacheKey) {
        String lockKey = LOCK_PREFIX + cacheKey;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked",
                        Objects.requireNonNull(LOCK_TTL, "LOCK_TTL must not be null"));
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Release the distributed lock after cache is rebuilt.
     */
    public void unlock(String cacheKey) {
        String lockKey = LOCK_PREFIX + cacheKey;
        redisTemplate.delete(lockKey);
    }
}
