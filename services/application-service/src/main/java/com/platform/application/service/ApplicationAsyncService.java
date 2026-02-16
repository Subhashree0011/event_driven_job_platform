package com.platform.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async operations for the application service.
 * Fire-and-forget tasks that don't need to block the HTTP response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationAsyncService {

    /**
     * Async notification to the user that their application was received.
     * In a real system, this would call the notification service via REST or Kafka.
     */
    @Async("taskExecutor")
    public void sendApplicationConfirmation(Long userId, Long jobId, Long applicationId) {
        log.info("Sending application confirmation: user={}, job={}, application={}",
                userId, jobId, applicationId);
        // The actual notification is handled by the notification-service
        // consuming the APPLICATION_CREATED event from Kafka
    }

    /**
     * Async cleanup of related caches when application state changes.
     */
    @Async("taskExecutor")
    public void invalidateRelatedCaches(Long jobId, Long userId) {
        log.debug("Invalidating caches for job={}, user={}", jobId, userId);
        // Would call Redis to invalidate application counts, etc.
    }
}
