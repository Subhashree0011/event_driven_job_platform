package com.platform.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Tracks retry behavior for failed notification deliveries.
 * 
 * Metrics exposed:
 *   - notification.retries.scheduled (Counter) — retries published to retry topic
 *   - notification.retries.success (Counter) — successful retries
 *   - notification.retries.failure (Counter) — failed retry attempts
 *   - notification.retries.dead_letter (Counter) — exhausted retries (dead letters)
 *   - notification.retries.publish_failure (Counter) — failed to even publish to retry topic
 * 
 * Dashboard insight: 
 *   dead_letter rate / scheduled rate = permanent failure ratio
 *   If > 10%, check downstream health (SMTP server, SMS API)
 */
@Component
public class RetryMetrics {

    private final MeterRegistry registry;

    public RetryMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRetryScheduled(String channel, int attempt) {
        Counter.builder("notification.retries.scheduled")
                .tag("channel", channel)
                .tag("attempt", String.valueOf(attempt))
                .description("Retry events scheduled")
                .register(registry)
                .increment();
    }

    public void recordRetrySuccess(String channel, int attempt) {
        Counter.builder("notification.retries.success")
                .tag("channel", channel)
                .tag("attempt", String.valueOf(attempt))
                .description("Successful retry deliveries")
                .register(registry)
                .increment();
    }

    public void recordRetryFailure(String channel, int attempt) {
        Counter.builder("notification.retries.failure")
                .tag("channel", channel)
                .tag("attempt", String.valueOf(attempt))
                .description("Failed retry attempts")
                .register(registry)
                .increment();
    }

    public void recordDeadLetter(String channel) {
        Counter.builder("notification.retries.dead_letter")
                .tag("channel", channel)
                .description("Dead letter events (all retries exhausted)")
                .register(registry)
                .increment();
    }

    public void recordRetryPublishFailure(String channel) {
        Counter.builder("notification.retries.publish_failure")
                .tag("channel", channel)
                .description("Failed to publish retry event to Kafka")
                .register(registry)
                .increment();
    }
}
