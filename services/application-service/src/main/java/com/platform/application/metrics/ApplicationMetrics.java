package com.platform.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom Prometheus metrics for application operations.
 */
@Component
@RequiredArgsConstructor
public class ApplicationMetrics {

    private final MeterRegistry meterRegistry;

    private Counter applicationsSubmitted;
    private Counter applicationsWithdrawn;
    private Counter statusChanges;
    private Counter duplicateAttempts;

    @PostConstruct
    public void init() {
        applicationsSubmitted = Counter.builder("application.submitted.total")
                .description("Total applications submitted")
                .register(meterRegistry);

        applicationsWithdrawn = Counter.builder("application.withdrawn.total")
                .description("Total applications withdrawn")
                .register(meterRegistry);

        statusChanges = Counter.builder("application.status_change.total")
                .description("Total status changes")
                .register(meterRegistry);

        duplicateAttempts = Counter.builder("application.duplicate.attempts")
                .description("Duplicate application attempts blocked")
                .register(meterRegistry);
    }

    public void recordSubmission() { applicationsSubmitted.increment(); }
    public void recordWithdrawal() { applicationsWithdrawn.increment(); }
    public void recordStatusChange() { statusChanges.increment(); }
    public void recordDuplicateAttempt() { duplicateAttempts.increment(); }
}
