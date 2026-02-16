package com.platform.user.resilience;

import com.platform.user.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Fallback handler for auth operations.
 * Called when circuit breakers open or bulkheads reject.
 * 
 * Strategy: Fail fast with a clear message rather than hanging.
 * "We prefer backpressure over memory death."
 */
@Slf4j
@Component
public class AuthFallbackHandler {

    public void handleDatabaseDown(Throwable t) {
        log.error("Auth fallback — database unavailable: {}", t.getMessage());
        throw new AuthException("Service temporarily unavailable. Please try again in a few moments.",
                HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }

    public void handleRedisDown(Throwable t) {
        log.warn("Auth fallback — Redis unavailable: {}", t.getMessage());
        // Redis being down shouldn't block auth — we can skip caching
        // Rate limiting will fail-open (allow requests)
    }
}
