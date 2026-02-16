package com.platform.job.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_search", columnList = "status, role, location, created_at"),
        @Index(name = "idx_jobs_company", columnList = "company_id"),
        @Index(name = "idx_jobs_employer", columnList = "employer_id"),
        @Index(name = "idx_jobs_type_level", columnList = "job_type, experience_level"),
        @Index(name = "idx_jobs_status_created", columnList = "status, created_at"),
        @Index(name = "idx_jobs_salary", columnList = "salary_min, salary_max")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(nullable = false, length = 100)
    private String role;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    @Builder.Default
    private JobType jobType = JobType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false)
    @Builder.Default
    private ExperienceLevel experienceLevel = ExperienceLevel.MID;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "application_count", nullable = false)
    @Builder.Default
    private Integer applicationCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    @Builder.Default
    private Set<String> skills = new HashSet<>();

    // === Business Methods ===

    public boolean isActive() {
        return status == JobStatus.ACTIVE;
    }

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDate.now());
    }

    public boolean isAcceptingApplications() {
        return isActive() && !isExpired();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementApplicationCount() {
        this.applicationCount++;
    }

    public void activate() {
        this.status = JobStatus.ACTIVE;
    }

    public void pause() {
        this.status = JobStatus.PAUSED;
    }

    public void close() {
        this.status = JobStatus.CLOSED;
    }

    // === Enums ===

    public enum JobType {
        FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, REMOTE
    }

    public enum ExperienceLevel {
        ENTRY, MID, SENIOR, LEAD, PRINCIPAL
    }

    public enum JobStatus {
        DRAFT, ACTIVE, PAUSED, CLOSED, EXPIRED
    }
}
