package com.platform.application.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Job application entity.
 * Unique constraint (user_id, job_id) prevents duplicate applications.
 * Uses READ COMMITTED isolation for write-heavy workload.
 */
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_user_job",
                columnNames = {"user_id", "job_id"}
        ),
        indexes = {
                @Index(name = "idx_applications_job", columnList = "job_id"),
                @Index(name = "idx_applications_user", columnList = "user_id"),
                @Index(name = "idx_applications_status", columnList = "status"),
                @Index(name = "idx_applications_job_status", columnList = "job_id, status"),
                @Index(name = "idx_applications_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // === Business Methods ===

    public boolean canBeWithdrawn() {
        return status == ApplicationStatus.SUBMITTED
                || status == ApplicationStatus.UNDER_REVIEW
                || status == ApplicationStatus.SHORTLISTED;
    }

    public boolean canTransitionTo(ApplicationStatus newStatus) {
        return switch (this.status) {
            case SUBMITTED -> newStatus == ApplicationStatus.UNDER_REVIEW
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
            case UNDER_REVIEW -> newStatus == ApplicationStatus.SHORTLISTED
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
            case SHORTLISTED -> newStatus == ApplicationStatus.INTERVIEW
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
            case INTERVIEW -> newStatus == ApplicationStatus.OFFERED
                    || newStatus == ApplicationStatus.REJECTED
                    || newStatus == ApplicationStatus.WITHDRAWN;
            case OFFERED -> newStatus == ApplicationStatus.WITHDRAWN;
            case REJECTED, WITHDRAWN -> false; // terminal states
        };
    }

    public void transitionTo(ApplicationStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", this.status, newStatus));
        }
        this.status = newStatus;
    }

    // === Enum ===

    public enum ApplicationStatus {
        SUBMITTED,
        UNDER_REVIEW,
        SHORTLISTED,
        INTERVIEW,
        OFFERED,
        REJECTED,
        WITHDRAWN
    }
}
