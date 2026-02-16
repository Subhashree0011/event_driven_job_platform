package com.platform.application.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Retry handler with exponential backoff for transient failures.
 *
 * Interview Talking Point:
 * - Exponential backoff: wait time doubles with each retry (1s, 2s, 4s, 8s, 16s)
 * - Jitter added to prevent thundering herd when many retries happen at the same time
 * - Max retries configurable to avoid infinite loops
 */
@Component
@Slf4j
public class RetryHandler {

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_DELAY_MS = 1000;

    /**
     * Calculate the delay for a given retry attempt using exponential backoff with jitter.
     *
     * @param attempt retry attempt number (0-based)
     * @return delay in milliseconds
     */
    public long calculateDelay(int attempt) {
        if (attempt >= MAX_RETRIES) {
            return -1; // signal to stop retrying
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        long exponentialDelay = INITIAL_DELAY_MS * (1L << attempt);

        // Add jitter (±25% of the delay)
        long jitter = (long) (exponentialDelay * 0.25 * (Math.random() * 2 - 1));

        return exponentialDelay + jitter;
    }

    /**
     * Check if we should retry based on the attempt count.
     */
    public boolean shouldRetry(int attempt) {
        return attempt < MAX_RETRIES;
    }
}
