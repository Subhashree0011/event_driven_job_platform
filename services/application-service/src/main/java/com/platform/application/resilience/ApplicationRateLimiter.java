package com.platform.application.resilience;

import com.platform.application.cache.RateLimitService;
import com.platform.application.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Application-level rate limiter for application endpoints.
 * Uses Redis sliding window counter (via RateLimitService).
 *
 * Limits (PRODUCTION mode only — bypassed when X-Test-Mode: true):
 * - Apply: 10 applications per 60s per user
 * - API: 100 requests per 60s per IP
 *
 * Interview Talking Point:
 * - Rate limiting protects against spam/abuse in production
 * - Testing mode bypasses limits to allow load testing
 * - Redis-based counter ensures distributed rate limiting across instances
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationRateLimiter {

    private final RateLimitService rateLimitService;

    private static final int APPLY_LIMIT_PER_USER = 10;
    private static final int API_LIMIT_PER_IP = 100;
    private static final long WINDOW_SECONDS = 60;

    /**
     * Check rate limit for job applications.
     * In test mode (X-Test-Mode header), rate limiting is bypassed.
     *
     * @param request HTTP request
     * @param userId  the applying user's ID
     */
    public void checkApplyRateLimit(HttpServletRequest request, Long userId) {
        if (isTestMode(request)) {
            log.debug("Test mode — skipping apply rate limit for user {}", userId);
            return;
        }

        String key = "apply:user:" + userId;
        if (!rateLimitService.isAllowed(key, APPLY_LIMIT_PER_USER, WINDOW_SECONDS)) {
            log.warn("Application rate limit exceeded for user: {}", userId);
            throw new RateLimitExceededException(
                    "Too many applications. You can submit up to " + APPLY_LIMIT_PER_USER
                            + " applications per minute. Please try again later.",
                    WINDOW_SECONDS);
        }
    }

    /**
     * Check rate limit per IP address for general API access.
     * In test mode, rate limiting is bypassed.
     */
    public void checkApiRateLimit(HttpServletRequest request) {
        if (isTestMode(request)) {
            return;
        }

        String clientIp = getClientIp(request);
        String key = "api:ip:" + clientIp;

        if (!rateLimitService.isAllowed(key, API_LIMIT_PER_IP, WINDOW_SECONDS)) {
            log.warn("API rate limit exceeded for IP: {}", clientIp);
            throw new RateLimitExceededException(
                    "Too many requests. Please slow down.", WINDOW_SECONDS);
        }
    }

    /**
     * Check if the request is in test mode (X-Test-Mode: true header).
     */
    private boolean isTestMode(HttpServletRequest request) {
        String testMode = request.getHeader("X-Test-Mode");
        return "true".equalsIgnoreCase(testMode);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
