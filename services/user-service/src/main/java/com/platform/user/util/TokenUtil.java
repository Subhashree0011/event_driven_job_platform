package com.platform.user.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility for generating secure random tokens.
 */
public final class TokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenUtil() {
        // Utility class
    }

    /**
     * Generate a cryptographically secure random token for refresh tokens.
     * Uses SecureRandom + Base64 encoding for URL-safe tokens.
     */
    public static String generateRefreshToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate a unique event/correlation ID.
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
