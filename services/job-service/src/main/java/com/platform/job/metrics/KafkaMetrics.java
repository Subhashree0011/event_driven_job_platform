package com.platform.job.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Kafka producer/consumer metrics for monitoring event pipeline health.
 */
@Component
@RequiredArgsConstructor
public class KafkaMetrics {

    private final MeterRegistry meterRegistry;

    private Counter publishSuccess;
    private Counter publishFailure;
    private Counter consumeSuccess;
    private Counter consumeFailure;

    @PostConstruct
    public void init() {
        publishSuccess = Counter.builder("job.kafka.publish.success")
                .description("Successful Kafka message publishes")
                .register(meterRegistry);

        publishFailure = Counter.builder("job.kafka.publish.failure")
                .description("Failed Kafka message publishes")
                .register(meterRegistry);

        consumeSuccess = Counter.builder("job.kafka.consume.success")
                .description("Successful Kafka message consumptions")
                .register(meterRegistry);

        consumeFailure = Counter.builder("job.kafka.consume.failure")
                .description("Failed Kafka message consumptions")
                .register(meterRegistry);
    }

    public void recordPublishSuccess() { publishSuccess.increment(); }
    public void recordPublishFailure() { publishFailure.increment(); }
    public void recordConsumeSuccess() { consumeSuccess.increment(); }
    public void recordConsumeFailure() { consumeFailure.increment(); }
}
