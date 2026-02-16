package com.platform.application.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when rate limit is exceeded.
 * Mapped to HTTP 429 Too Many Requests.
 *
 * In TESTING mode (X-Test-Mode: true), rate limiting is bypassed.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 60;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
