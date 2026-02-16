package com.platform.job.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback handler for cache operations when Redis is unavailable.
 * Logs the failure and allows the request to proceed without cache.
 */
@Component
@Slf4j
public class CacheFallbackHandler {

    public void handleCacheFailure(String operation, String key, Throwable cause) {
        log.warn("Cache operation '{}' failed for key '{}': {}. Falling back to database.",
                operation, key, cause.getMessage());
    }

    public void handleCacheWriteFailure(String key, Throwable cause) {
        log.warn("Failed to write to cache for key '{}': {}. Data is in DB, cache will be populated on next read.",
                key, cause.getMessage());
    }
}
