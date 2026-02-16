package com.platform.user.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles cache invalidation events.
 * In a microservice architecture, cache invalidation can be triggered by:
 * 1. Direct update (write-through)
 * 2. Kafka events from other services
 * 3. TTL expiry (passive)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationHandler {

    private final UserCacheService userCacheService;

    /**
     * Invalidate user cache — called on user updates or deletion.
     */
    public void invalidateUser(Long userId) {
        userCacheService.evictUser(userId);
        log.debug("Cache invalidated for user: {}", userId);
    }
}
