package com.platform.application.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bulkhead for application submission.
 * Limits concurrent application processing to prevent DB connection exhaustion.
 * Configuration: max 25 concurrent calls (via application.yml).
 */
@Component
@Slf4j
public class ApplicationBulkhead {

    public void handleBulkheadFull() {
        log.warn("Application bulkhead is full — submission rejected due to high load.");
    }
}
