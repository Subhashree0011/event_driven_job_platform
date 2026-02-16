package com.platform.application.service;

import com.platform.application.model.Application;
import com.platform.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Validates application business rules before allowing mutations.
 *
 * Interview Talking Point:
 * - Separated from command service for single responsibility
 * - Validation is synchronous and must complete before DB write
 * - Cross-service validation (e.g., "does job exist?") could use REST call or cache
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationValidationService {

    private final ApplicationRepository applicationRepository;

    /**
     * Validate that the user hasn't already applied to this job.
     * Uses unique constraint as a safety net, but we check programmatically
     * for a better error message.
     */
    public void validateNewApplication(Long userId, Long jobId) {
        if (applicationRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new com.platform.application.exception.ApplicationException(
                    "DUPLICATE_APPLICATION",
                    String.format("User %d has already applied to job %d", userId, jobId),
                    org.springframework.http.HttpStatus.CONFLICT
            );
        }
    }

    /**
     * Validate that a status transition is allowed.
     */
    public void validateStatusTransition(Application application, Application.ApplicationStatus newStatus) {
        if (!application.canTransitionTo(newStatus)) {
            throw new com.platform.application.exception.ApplicationException(
                    "INVALID_STATUS_TRANSITION",
                    String.format("Cannot transition from %s to %s",
                            application.getStatus(), newStatus),
                    org.springframework.http.HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Validate that the user owns this application (for withdrawal).
     */
    public void validateOwnership(Application application, Long userId) {
        if (!application.getUserId().equals(userId)) {
            throw new com.platform.application.exception.ApplicationException(
                    "UNAUTHORIZED",
                    "You are not authorized to modify this application",
                    org.springframework.http.HttpStatus.FORBIDDEN
            );
        }
    }
}
