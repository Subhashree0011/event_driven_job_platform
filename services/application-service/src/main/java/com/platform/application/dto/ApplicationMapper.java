package com.platform.application.dto;

import com.platform.application.model.Application;

/**
 * Maps between Application entity and DTOs.
 */
public final class ApplicationMapper {

    private ApplicationMapper() {
        // Utility class
    }

    public static ApplicationResponse toResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .userId(application.getUserId())
                .status(application.getStatus())
                .coverLetter(application.getCoverLetter())
                .resumeUrl(application.getResumeUrl())
                .notes(application.getNotes())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    public static Application toEntity(CreateApplicationRequest request, Long userId) {
        return Application.builder()
                .jobId(request.getJobId())
                .userId(userId)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(request.getResumeUrl())
                .build();
    }
}
