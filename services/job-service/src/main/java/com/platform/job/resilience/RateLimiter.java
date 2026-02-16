package com.platform.job.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rate limiter handler.
 * Configuration is declarative via application.yml (resilience4j.ratelimiter.instances).
 *
 * Configured: jobSearchRateLimiter — 50 requests per 60 seconds.
 * This protects the search endpoint from abuse and ensures fair usage.
 */
@Component
@Slf4j
public class RateLimiter {

    public void handleRateLimitExceeded(String limiterName) {
        log.warn("Rate limit exceeded for '{}'. Request throttled.", limiterName);
    }
}
