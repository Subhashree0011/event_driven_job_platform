package com.platform.job.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Database operation metrics — tracks query latency and connection pool health.
 */
@Component
@RequiredArgsConstructor
public class DbMetrics {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // Pre-register the base timer to ensure it exists in the registry
        Timer.builder("job.db.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);
    }

    public Timer.Sample startQuery() {
        return Timer.start(meterRegistry);
    }

    public void stopQuery(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("job.db.query.duration")
                .tag("queryType", queryType)
                .register(meterRegistry));
    }
}
