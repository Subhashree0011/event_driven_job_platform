package com.platform.notification.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for Notification Service.
 * 
 * Adds common tags to all metrics for Prometheus/Grafana filtering.
 * All notification-specific counters/gauges are registered in their respective
 * metric classes (NotificationLatencyMetrics, FailureRateMetrics, RetryMetrics).
 */
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags(
                        "service", "notification-service",
                        "team", "platform"
                );
    }
}
