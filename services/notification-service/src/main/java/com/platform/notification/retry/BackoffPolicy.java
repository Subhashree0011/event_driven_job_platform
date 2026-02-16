package com.platform.notification.retry;

import com.platform.notification.config.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Backoff policy for notification retries.
 * 
 * Implements exponential backoff with jitter to prevent retry storms.
 * 
 * Without jitter: All failed notifications retry at the same instant → thundering herd.
 * With jitter: Retries are spread across a time window → gradual recovery.
 * 
 * Formula: delay = min(initialInterval * multiplier^attempt + jitter, maxInterval)
 * Jitter: Random value between 0 and 20% of the calculated delay
 */
@Component
@RequiredArgsConstructor
public class BackoffPolicy {

    private final RetryConfig retryConfig;

    /**
     * Calculate the backoff delay for a given attempt, with jitter.
     * 
     * @param attempt The retry attempt number (1-based)
     * @return Delay in milliseconds before the next retry
     */
    public long calculateDelay(int attempt) {
        long baseDelay = retryConfig.getRetry().calculateDelay(attempt);

        // Add ±20% jitter to prevent thundering herd
        double jitterFactor = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        long jitteredDelay = (long) (baseDelay * jitterFactor);

        return Math.min(jitteredDelay, retryConfig.getRetry().getMaxInterval());
    }

    /**
     * Check if the given attempt number is within retry limits.
     */
    public boolean canRetry(int attempt) {
        return attempt < retryConfig.getRetry().getMaxAttempts();
    }

    /**
     * Get the maximum allowed retry attempts.
     */
    public int getMaxAttempts() {
        return retryConfig.getRetry().getMaxAttempts();
    }
}
