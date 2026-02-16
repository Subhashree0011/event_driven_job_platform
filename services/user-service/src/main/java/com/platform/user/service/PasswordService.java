package com.platform.user.service;

import com.platform.user.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Password hashing and verification service.
 * Uses BCrypt with strength 12 — each hash ~250ms, which also
 * acts as natural brute-force protection.
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final HashUtil hashUtil;

    public String hashPassword(String rawPassword) {
        return hashUtil.hash(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashUtil.matches(rawPassword, hashedPassword);
    }
}
