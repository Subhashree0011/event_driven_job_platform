package com.platform.user.resilience;

import com.platform.user.cache.RateLimitService;
import com.platform.user.exception.RateLimitExceededException;
import com.platform.user.metrics.RateLimitMetrics;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Application-level rate limiter for auth endpoints.
 * Uses Redis sliding window counter (via RateLimitService).
 * 
 * Limits:
 * - Login: 5 attempts per 60s per IP
 * - Register: 5 attempts per 60s per IP
 * - API: 100 requests per 60s per user
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimiter {

    private final RateLimitService rateLimitService;
    private final RateLimitMetrics rateLimitMetrics;

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 5;
    private static final long WINDOW_SECONDS = 60;

    public void checkLoginRateLimit(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String key = "login:" + clientIp;

        if (!rateLimitService.isAllowed(key, LOGIN_LIMIT, WINDOW_SECONDS)) {
            rateLimitMetrics.recordRateLimitHit();
            log.warn("Login rate limit exceeded for IP: {}", clientIp);
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again later.", WINDOW_SECONDS);
        }
    }

    public void checkRegisterRateLimit(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String key = "register:" + clientIp;

        if (!rateLimitService.isAllowed(key, REGISTER_LIMIT, WINDOW_SECONDS)) {
            rateLimitMetrics.recordRateLimitHit();
            throw new RateLimitExceededException(
                    "Too many registration attempts. Please try again later.", WINDOW_SECONDS);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
