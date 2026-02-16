package com.platform.user.controller;

import com.platform.user.dto.UpdateProfileRequest;
import com.platform.user.dto.UserResponse;
import com.platform.user.security.SecurityContextProvider;
import com.platform.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User profile management endpoints.
 * All endpoints require authentication (enforced by SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityContextProvider securityContextProvider;

    /**
     * Get current user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        Long userId = securityContextProvider.getCurrentUserId();
        UserResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a user's profile by ID (public info).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
        UserResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityContextProvider.getCurrentUserId();
        UserResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
