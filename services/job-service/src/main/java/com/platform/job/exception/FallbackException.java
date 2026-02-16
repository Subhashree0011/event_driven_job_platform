package com.platform.job.exception;

/**
 * Thrown when a circuit breaker fallback cannot serve the request.
 */
public class FallbackException extends RuntimeException {

    public FallbackException(String message) {
        super(message);
    }

    public FallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
