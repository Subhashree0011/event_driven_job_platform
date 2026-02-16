package com.platform.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility for password hashing using BCrypt.
 * BCrypt includes salt automatically, so no separate salt management needed.
 */
@Component
public class HashUtil {

    private final PasswordEncoder passwordEncoder;

    public HashUtil() {
        // Strength 12 — good balance between security and performance
        // Each hash takes ~250ms, which also acts as natural rate limiting
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    public PasswordEncoder getEncoder() {
        return passwordEncoder;
    }
}
