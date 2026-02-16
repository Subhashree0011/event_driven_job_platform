package com.platform.application.outbox;

import com.platform.application.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox poller — reads unpublished events from the outbox table
 * and delegates to OutboxPublisher to publish them to Kafka.
 *
 * Runs on a fixed schedule (default: every 1 second).
 * Processes events in batches to avoid overwhelming Kafka.
 *
 * Interview Talking Point:
 * - Polling-based approach is simpler than CDC (Change Data Capture)
 * - Trade-off: polling adds latency (up to 1 second) vs CDC which is near-realtime
 * - Batch processing prevents DB connection starvation
 * - Retry count prevents poison pill events from blocking the pipeline
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final MeterRegistry meterRegistry;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Value("${outbox.retry-max-attempts:5}")
    private int maxRetryAttempts;

    /**
     * Poll for unpublished outbox events every second.
     * Uses @Scheduled with fixedDelayString to avoid overlap if processing takes > 1 second.
     */
    @Scheduled(fixedDelayString = "${outbox.polling-interval:1000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository
                .findUnpublishedEvents(maxRetryAttempts, batchSize);

        if (events.isEmpty()) {
            return; // Nothing to process — no log noise
        }

        log.debug("Polling outbox: found {} unpublished events", events.size());
        int published = 0;
        int failed = 0;

        for (OutboxEvent event : events) {
            try {
                outboxPublisher.publish(event);
                event.markPublished();
                outboxEventRepository.save(event);
                published++;
            } catch (Exception e) {
                event.incrementRetryCount();
                outboxEventRepository.save(event);
                failed++;

                if (event.getRetryCount() >= maxRetryAttempts) {
                    log.error("Outbox event {} exceeded max retries ({}). Event will not be retried. " +
                                    "Aggregate: {}:{}, EventType: {}",
                            event.getId(), maxRetryAttempts,
                            event.getAggregateType(), event.getAggregateId(),
                            event.getEventType());
                    meterRegistry.counter("outbox.event.dead_letter").increment();
                } else {
                    log.warn("Failed to publish outbox event {} (attempt {}/{}): {}",
                            event.getId(), event.getRetryCount(), maxRetryAttempts,
                            e.getMessage());
                }
            }
        }

        if (published > 0 || failed > 0) {
            log.info("Outbox poll complete: published={}, failed={}", published, failed);
            meterRegistry.counter("outbox.event.published").increment(published);
            meterRegistry.counter("outbox.event.failed").increment(failed);
        }
    }
}
