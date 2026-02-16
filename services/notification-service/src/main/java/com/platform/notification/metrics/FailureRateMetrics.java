package com.platform.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Tracks notification failure rates by channel and failure type.
 * 
 * Metrics exposed:
 *   - notification.failures (Counter) — delivery failures by channel + reason
 *   - notification.duplicates (Counter) — duplicate events detected
 *   - notification.dedup.failures (Counter) — deduplication mechanism failures
 * 
 * Used for:
 *   - Alerting on sudden failure rate spikes
 *   - Identifying problematic channels
 *   - Monitoring deduplication effectiveness
 */
@Component
public class FailureRateMetrics {

    private final MeterRegistry registry;

    public FailureRateMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record a notification delivery failure.
     */
    public void recordFailure(String channel, String reason) {
        Counter.builder("notification.failures")
                .tag("channel", channel)
                .tag("reason", reason)
                .description("Notification delivery failures")
                .register(registry)
                .increment();
    }

    /**
     * Record a duplicate event detection.
     */
    public void recordDuplicate(String source) {
        Counter.builder("notification.duplicates")
                .tag("source", source)
                .description("Duplicate notification events detected")
                .register(registry)
                .increment();
    }

    /**
     * Record a deduplication mechanism failure (e.g., missing event ID).
     */
    public void recordDeduplicationFailure(String reason) {
        Counter.builder("notification.dedup.failures")
                .tag("reason", reason)
                .description("Deduplication mechanism failures")
                .register(registry)
                .increment();
    }
}
