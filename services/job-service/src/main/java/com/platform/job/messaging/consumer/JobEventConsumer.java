package com.platform.job.messaging.consumer;

import com.platform.job.repository.JobRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes events from other services that affect jobs.
 * Currently listens for application events to update application_count.
 *
 * Interview Talking Point:
 * - Consumer group "job-service-group" — each partition assigned to one instance
 * - Processes application.created events to denormalize application_count
 * - Denormalization avoids cross-service queries at read time
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventConsumer {

    private final JobRepository jobRepository;
    private final MeterRegistry meterRegistry;

    /**
     * When an application is submitted (published by application-service),
     * increment the application_count on the job.
     * This is a denormalization — we trade consistency for read performance.
     */
    @KafkaListener(
            topics = "application.created",
            groupId = "job-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleApplicationCreated(Map<String, Object> event) {
        try {
            Object jobIdObj = event.get("jobId");
            if (jobIdObj == null) {
                log.warn("Received application.created event without jobId: {}", event);
                return;
            }

            Long jobId = Long.valueOf(jobIdObj.toString());
            jobRepository.incrementApplicationCount(jobId);

            log.debug("Incremented application count for job {}", jobId);
            meterRegistry.counter("kafka.consume.success",
                    "topic", "application.created").increment();

        } catch (Exception e) {
            log.error("Error processing application.created event: {}", e.getMessage(), e);
            meterRegistry.counter("kafka.consume.error",
                    "topic", "application.created").increment();
            // Don't rethrow — dead letter topic handling would go here
            // For now, log and continue to avoid blocking the partition
        }
    }
}
