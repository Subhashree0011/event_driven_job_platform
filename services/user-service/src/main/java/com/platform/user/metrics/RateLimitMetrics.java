package com.platform.user.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Rate limiting metrics for monitoring and alerting.
 */
@Component
public class RateLimitMetrics {

    private final Counter rateLimitHits;

    public RateLimitMetrics(MeterRegistry registry) {
        this.rateLimitHits = Counter.builder("auth.ratelimit.hits")
                .description("Number of requests blocked by rate limiting")
                .register(registry);
    }

    public void recordRateLimitHit() { rateLimitHits.increment(); }
}
