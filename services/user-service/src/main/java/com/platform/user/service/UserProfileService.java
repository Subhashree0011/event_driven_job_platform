package com.platform.user.service;

import com.platform.user.cache.UserCacheService;
import com.platform.user.dto.UpdateProfileRequest;
import com.platform.user.dto.UserMapper;
import com.platform.user.dto.UserResponse;
import com.platform.user.exception.AuthException;
import com.platform.user.model.User;
import com.platform.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * User profile management service.
 * Uses write-through caching — updates go to both DB and Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserCacheService userCacheService;
    private final UserMapper userMapper;

    /**
     * Get user profile by ID.
     * Write-through cache: check Redis first, then DB.
     */
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "getProfileFallback")
    public UserResponse getProfile(Long userId) {
        // Try cache first (write-through pattern)
        UserResponse cached = userCacheService.getCachedUser(userId);
        if (cached != null) {
            log.debug("Cache hit for user: {}", userId);
            return cached;
        }

        // Cache miss — load from DB
        log.debug("Cache miss for user: {}", userId);
        User user = userRepository.findById(Objects.requireNonNull(userId, "userId must not be null"))
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        UserResponse response = userMapper.toResponse(user);

        // Write to cache
        userCacheService.cacheUser(userId, response);

        return response;
    }

    /**
     * Update user profile.
     * Write-through: update DB first, then invalidate and re-cache.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(Objects.requireNonNull(userId, "userId must not be null"))
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // Apply updates (only non-null fields)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfilePictureUrl() != null) user.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getResumeUrl() != null) user.setResumeUrl(request.getResumeUrl());
        if (request.getLocation() != null) user.setLocation(request.getLocation());

        @SuppressWarnings("null")
        User savedUser = Objects.requireNonNull(userRepository.save(user), "User save returned null");
        user = savedUser;

        UserResponse response = userMapper.toResponse(user);

        // Write-through: update cache immediately after DB write
        userCacheService.cacheUser(userId, response);

        log.info("Profile updated for user: {}", userId);
        return response;
    }

    // ==================== Fallback ====================

    @SuppressWarnings("unused")
    private UserResponse getProfileFallback(Long userId, Throwable t) {
        log.warn("Profile circuit breaker — attempting stale cache for user: {}", userId);
        // Try to serve stale cached data
        UserResponse cached = userCacheService.getCachedUser(userId);
        if (cached != null) {
            return cached;
        }
        throw new AuthException("Service temporarily unavailable",
                HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }
}
