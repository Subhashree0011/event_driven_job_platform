package com.platform.user.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Session cache — tracks active sessions in Redis.
 * Used for session counting and forced logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCache {

    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public void createSession(Long userId, String sessionId) {
        String key = SESSION_PREFIX + userId + ":" + sessionId;
        redisTemplate.opsForValue().set(key, "active", SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    public void invalidateSession(Long userId, String sessionId) {
        String key = SESSION_PREFIX + userId + ":" + sessionId;
        redisTemplate.delete(key);
    }

    public boolean isSessionActive(Long userId, String sessionId) {
        String key = SESSION_PREFIX + userId + ":" + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
