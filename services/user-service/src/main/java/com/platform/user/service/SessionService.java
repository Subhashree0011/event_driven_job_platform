package com.platform.user.service;

import com.platform.user.model.RefreshToken;
import com.platform.user.model.User;
import com.platform.user.repository.RefreshTokenRepository;
import com.platform.user.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages user sessions through refresh tokens.
 * Refresh tokens are stored in DB, access tokens are stateless JWTs.
 * 
 * Design: Stateless access tokens (short-lived) + stateful refresh tokens (long-lived)
 * This allows token revocation without checking DB on every request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    /**
     * Create a new refresh token for a user.
     * Revokes all existing tokens first (single active session).
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens — enforce single active session
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(TokenUtil.generateRefreshToken())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .revoked(false)
                .build();

        @SuppressWarnings("null")
        RefreshToken saved = Objects.requireNonNull(refreshTokenRepository.save(refreshToken), "RefreshToken save returned null");
        return saved;
    }

    /**
     * Validate and return a refresh token.
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(RefreshToken::isUsable);
    }

    /**
     * Revoke a specific refresh token.
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Cleanup expired tokens — runs every hour.
     * Prevents the refresh_tokens table from growing unbounded.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens();
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}
