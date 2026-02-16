package com.platform.user.service;

import com.platform.user.dto.*;
import com.platform.user.exception.AuthException;
import com.platform.user.model.RefreshToken;
import com.platform.user.model.User;
import com.platform.user.repository.UserRepository;
import com.platform.user.security.TokenBlacklistService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Core authentication service — handles registration, login, token refresh, and logout.
 * 
 * Design Decisions:
 * - Stateless access tokens (JWT) + stateful refresh tokens (DB)
 * - BCrypt with strength 12 for password hashing
 * - Token blacklisting via Redis for immediate revocation
 * - Single active session enforced via refresh token rotation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    /**
     * Register a new user.
     */
    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "registerFallback")
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }

        // Hash password (BCrypt strength 12 — ~250ms, acts as natural rate limiting)
        String hashedPassword = passwordService.hashPassword(request.getPassword());

        // Create user entity
        User user = userMapper.toEntity(request, hashedPassword);
        @SuppressWarnings("null")
        User savedUser = Objects.requireNonNull(userRepository.save(user), "User save returned null");
        user = savedUser;

        log.info("User registered: {} (role: {})", user.getEmail(), user.getRole());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Authenticate user and issue tokens.
     */
    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "loginFallback")
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthException("Invalid email or password", "INVALID_CREDENTIALS"));

        // Check account status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("Account is " + user.getStatus().name().toLowerCase(),
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        // Verify password
        if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for: {}", request.getEmail());
            throw new AuthException("Invalid email or password", "INVALID_CREDENTIALS");
        }

        log.info("User logged in: {}", user.getEmail());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using a valid refresh token.
     * Implements refresh token rotation — old token is revoked, new one issued.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = sessionService.validateRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid or expired refresh token", "INVALID_REFRESH_TOKEN"));

        User user = refreshToken.getUser();

        // Revoke old refresh token (rotation)
        sessionService.revokeToken(request.getRefreshToken());

        log.debug("Token refreshed for: {}", user.getEmail());

        // Issue new tokens
        return generateAuthResponse(user);
    }

    /**
     * Logout — revoke refresh token and blacklist access token.
     */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // Blacklist the access token in Redis (TTL = remaining token lifetime)
        if (accessToken != null) {
            long ttlSeconds = jwtService.getAccessTokenExpiration() / 1000;
            tokenBlacklistService.blacklist(accessToken, ttlSeconds);
        }

        // Revoke the refresh token in DB
        if (refreshToken != null) {
            sessionService.revokeToken(refreshToken);
        }

        log.debug("User logged out");
    }

    private AuthResponse generateAuthResponse(User user) {
        // Generate access token (stateless JWT)
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        // Create refresh token (stateful, stored in DB)
        RefreshToken refreshToken = sessionService.createRefreshToken(user);

        // Map to response
        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpiration() / 1000,
                userResponse);
    }

    // ==================== Fallback Methods ====================

    @SuppressWarnings("unused")
    private AuthResponse registerFallback(RegisterRequest request, Throwable t) {
        log.error("Registration circuit breaker triggered: {}", t.getMessage());
        throw new AuthException("Service temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }

    @SuppressWarnings("unused")
    private AuthResponse loginFallback(LoginRequest request, Throwable t) {
        log.error("Login circuit breaker triggered: {}", t.getMessage());
        throw new AuthException("Service temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }
}
