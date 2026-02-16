package com.platform.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token blacklist for JWT revocation.
 * 
 * Design: Since JWTs are stateless, we can't "invalidate" them.
 * Instead, we maintain a blacklist in Redis.
 * 
 * The blacklist entry expires when the token would have expired anyway,
 * so Redis memory stays bounded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Add a token to the blacklist.
     * @param token The JWT to blacklist
     * @param ttlSeconds Time until the token would naturally expire
     */
    public void blacklist(String token, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Token blacklisted, TTL: {}s", ttlSeconds);
    }

    /**
     * Check if a token is blacklisted.
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
