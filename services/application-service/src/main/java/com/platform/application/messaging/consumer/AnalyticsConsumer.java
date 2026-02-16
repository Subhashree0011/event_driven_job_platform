package com.platform.application.messaging.consumer;

import com.platform.application.idempotency.DuplicateEventDetector;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes job lifecycle events from the job-service.
 * Used to validate that jobs still exist and are active
 * when processing applications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final DuplicateEventDetector duplicateEventDetector;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "job.lifecycle",
            groupId = "application-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleJobLifecycleEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Object jobIdObj = event.get("jobId");

        if (eventType == null || jobIdObj == null) {
            log.warn("Received malformed job lifecycle event: {}", event);
            return;
        }

        String eventId = eventType + ":" + jobIdObj + ":" + event.getOrDefault("timestamp", "");

        // Idempotent processing
        if (!duplicateEventDetector.isNewEvent(eventId)) {
            log.debug("Skipping duplicate event: {}", eventId);
            return;
        }

        try {
            Long jobId = Long.valueOf(jobIdObj.toString());

            switch (eventType) {
                case "JOB_CLOSED", "JOB_STATUS_CHANGED" -> {
                    String status = (String) event.get("status");
                    log.info("Job {} status changed to {}. Applications may be affected.", jobId, status);
                    // Could auto-reject pending applications for closed jobs
                }
                default -> log.debug("Received job lifecycle event: type={}, jobId={}", eventType, jobId);
            }

            duplicateEventDetector.markProcessed(eventId);
            meterRegistry.counter("kafka.consume.success",
                    "topic", "job.lifecycle", "eventType", eventType).increment();

        } catch (Exception e) {
            log.error("Error processing job lifecycle event: {}", e.getMessage(), e);
            meterRegistry.counter("kafka.consume.error",
                    "topic", "job.lifecycle").increment();
        }
    }
}
