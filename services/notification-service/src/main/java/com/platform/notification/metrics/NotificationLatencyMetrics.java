package com.platform.notification.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Tracks notification delivery latency by channel and event type.
 * 
 * Metrics exposed:
 *   - notification.send.latency (Timer) — end-to-end delivery time
 *   - notification.processing.latency (Timer) — Kafka event processing time
 *   - notification.circuitbreaker.open (Counter) — circuit breaker activations
 * 
 * Grafana query examples:
 *   histogram_quantile(0.99, rate(notification_send_latency_seconds_bucket[5m]))
 *   rate(notification_circuitbreaker_open_total[5m])
 */
@Component
public class NotificationLatencyMetrics {

    private final MeterRegistry registry;

    public NotificationLatencyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record time taken to send a notification via a specific channel.
     */
    public void recordSendLatency(String channel, long durationMs) {
        Timer.builder("notification.send.latency")
                .tag("channel", channel)
                .description("Notification delivery latency by channel")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record time taken to process a Kafka event end-to-end.
     */
    public void recordProcessingLatency(String channel, String eventType, long durationMs) {
        Timer.builder("notification.processing.latency")
                .tag("channel", channel)
                .tag("event_type", eventType)
                .description("Kafka event processing latency")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record circuit breaker open event.
     */
    public void recordCircuitBreakerOpen(String channel) {
        Counter.builder("notification.circuitbreaker.open")
                .tag("channel", channel)
                .description("Circuit breaker open events")
                .register(registry)
                .increment();
    }
}
