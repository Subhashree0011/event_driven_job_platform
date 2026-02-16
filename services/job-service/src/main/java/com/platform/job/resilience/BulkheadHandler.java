package com.platform.job.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bulkhead handler — limits concurrent access to protect resources.
 * Configuration is declarative via application.yml (resilience4j.bulkhead.instances).
 *
 * Interview Talking Point:
 * - Bulkhead pattern isolates different workloads (like watertight compartments in a ship)
 * - jobSearchBulkhead: max 30 concurrent search requests
 * - Prevents a slow search query from consuming all server threads
 */
@Component
@Slf4j
public class BulkheadHandler {

    /**
     * Called when bulkhead rejects a request because max concurrent calls reached.
     */
    public void handleBulkheadFull(String bulkheadName) {
        log.warn("Bulkhead '{}' is full — request rejected. System is under heavy load.", bulkheadName);
    }
}
