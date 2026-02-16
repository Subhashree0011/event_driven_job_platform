package com.platform.job.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom Prometheus metrics for cache performance monitoring.
 * Tracks hit ratio, miss ratio, and cache operation latency.
 */
@Component
@RequiredArgsConstructor
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    private Counter cacheHits;
    private Counter cacheMisses;
    private Counter cacheErrors;
    private Timer cacheLatency;

    @PostConstruct
    public void init() {
        cacheHits = Counter.builder("job.cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);

        cacheMisses = Counter.builder("job.cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);

        cacheErrors = Counter.builder("job.cache.errors")
                .description("Number of cache errors")
                .register(meterRegistry);

        cacheLatency = Timer.builder("job.cache.latency")
                .description("Cache operation latency")
                .register(meterRegistry);
    }

    public void recordHit() {
        cacheHits.increment();
    }

    public void recordMiss() {
        cacheMisses.increment();
    }

    public void recordError() {
        cacheErrors.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(cacheLatency);
    }
}
