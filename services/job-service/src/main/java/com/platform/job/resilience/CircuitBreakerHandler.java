package com.platform.job.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Programmatic circuit breaker handler for custom fallback logic.
 * Monitors circuit breaker state transitions for alerting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerHandler {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get the current state of a circuit breaker by name.
     */
    public CircuitBreaker.State getState(String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        return cb.getState();
    }

    /**
     * Check if a circuit breaker is in OPEN state (all requests blocked).
     */
    public boolean isOpen(String name) {
        return getState(name) == CircuitBreaker.State.OPEN;
    }

    /**
     * Reset a circuit breaker to CLOSED state (for manual intervention).
     */
    public void reset(String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.reset();
        log.info("Circuit breaker '{}' manually reset to CLOSED", name);
    }
}
