package com.platform.job.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Defines caching policies and TTL strategies for different data types.
 * Centralized cache policy prevents inconsistent TTLs across the codebase.
 */
@Component
@Slf4j
public class CachePolicy {

    // Search results — short TTL because new jobs are posted frequently
    public static final long SEARCH_TTL_SECONDS = 60;

    // Job detail — medium TTL because individual jobs change infrequently
    public static final long DETAIL_TTL_SECONDS = 300;

    // Company data — long TTL because company info rarely changes
    public static final long COMPANY_TTL_SECONDS = 1800;

    // Jitter range to prevent thundering herd
    public static final long TTL_JITTER_SECONDS = 10;

    /**
     * Calculate TTL with jitter.
     * Jitter adds randomness to prevent all cache entries from expiring at the same time.
     */
    public static long withJitter(long baseTtlSeconds) {
        long jitter = (long) (Math.random() * TTL_JITTER_SECONDS * 2) - TTL_JITTER_SECONDS;
        return Math.max(1, baseTtlSeconds + jitter);
    }
}
