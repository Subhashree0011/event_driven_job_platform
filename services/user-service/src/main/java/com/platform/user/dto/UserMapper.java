package com.platform.user.dto;

import com.platform.user.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps between User entity and DTOs.
 * Keeping mapping explicit rather than using MapStruct for transparency.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .resumeUrl(user.getResumeUrl())
                .location(user.getLocation())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User toEntity(RegisterRequest request, String passwordHash) {
        User.UserRole role = User.UserRole.JOB_SEEKER;
        if (request.getRole() != null) {
            try {
                role = User.UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Default to JOB_SEEKER if invalid role provided
            }
        }

        return User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
    }
}
