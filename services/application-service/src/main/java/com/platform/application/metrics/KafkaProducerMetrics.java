package com.platform.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Kafka producer metrics for monitoring outbox publishing health.
 */
@Component("applicationKafkaProducerMetrics")
@RequiredArgsConstructor
public class KafkaProducerMetrics {

    private final MeterRegistry meterRegistry;

    private Counter outboxPublished;
    private Counter outboxFailed;
    private Counter outboxRetried;

    @PostConstruct
    public void init() {
        outboxPublished = Counter.builder("outbox.published.total")
                .description("Total outbox events successfully published to Kafka")
                .register(meterRegistry);

        outboxFailed = Counter.builder("outbox.failed.total")
                .description("Total outbox events that failed publishing")
                .register(meterRegistry);

        outboxRetried = Counter.builder("outbox.retried.total")
                .description("Total outbox events retried")
                .register(meterRegistry);
    }

    public void recordPublished() { outboxPublished.increment(); }
    public void recordFailed() { outboxFailed.increment(); }
    public void recordRetried() { outboxRetried.increment(); }
}
