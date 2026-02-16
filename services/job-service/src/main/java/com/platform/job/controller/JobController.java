package com.platform.job.controller;

import com.platform.job.dto.*;
import com.platform.job.service.CacheAwareJobService;
import com.platform.job.service.JobCommandService;
import com.platform.job.service.JobQueryService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Job CRUD and search operations.
 *
 * Interview Talking Point:
 * - CQRS separation: reads go through CacheAwareJobService (cache-aside),
 * writes go through JobCommandService (invalidation + Kafka events)
 * - Rate limiting on search endpoint to prevent abuse
 * - employerId extracted from JWT header (set by API Gateway)
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobCommandService jobCommandService;
    private final CacheAwareJobService cacheAwareJobService;
    private final JobQueryService jobQueryService;

    // ==================== Search & Read ====================

    /**
     * Search jobs with filters and pagination.
     * Uses cache-aside pattern — results cached for 60s with jitter.
     */
    @GetMapping("/search")
    @RateLimiter(name = "jobSearchRateLimiter")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        JobSearchRequest request = JobSearchRequest.builder()
                .keyword(keyword)
                .location(location)
                .role(role)
                .jobType(jobType != null ? com.platform.job.model.Job.JobType.valueOf(jobType.toUpperCase()) : null)
                .experienceLevel(experienceLevel != null
                        ? com.platform.job.model.Job.ExperienceLevel.valueOf(experienceLevel.toUpperCase())
                        : null)
                .status(status != null ? com.platform.job.model.Job.JobStatus.valueOf(status.toUpperCase()) : null)
                .companyId(companyId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<JobResponse> results = cacheAwareJobService.searchJobsCached(request);
        return ResponseEntity.ok(results);
    }

    /**
     * Get job by ID.
     * Uses cache-aside with 300s TTL for individual job details.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long jobId) {
        // Record view asynchronously (fire-and-forget)
        jobQueryService.recordView(jobId);

        JobResponse job = cacheAwareJobService.getJobCached(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get jobs by employer (for employer dashboard).
     */
    @GetMapping("/employer/{employerId}")
    public ResponseEntity<Page<JobResponse>> getJobsByEmployer(
            @PathVariable Long employerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobQueryService.getJobsByEmployer(employerId, page, size));
    }

    /**
     * Get jobs by company.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<JobResponse>> getJobsByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobQueryService.getJobsByCompany(companyId, page, size));
    }

    // ==================== Create & Update ====================

    /**
     * Create a new job posting.
     * Employer ID is extracted from the X-User-Id header (set by API Gateway after
     * JWT validation).
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader("X-User-Id") Long employerId) {
        JobResponse job = jobCommandService.createJob(request, employerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    /**
     * Update an existing job.
     */
    @PutMapping("/{jobId}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateJobRequest request,
            @RequestHeader("X-User-Id") Long employerId) {
        JobResponse job = jobCommandService.updateJob(jobId, request, employerId);
        return ResponseEntity.ok(job);
    }

    // ==================== Status Changes ====================

    @PutMapping("/{jobId}/activate")
    public ResponseEntity<JobResponse> activateJob(
            @PathVariable Long jobId,
            @RequestHeader("X-User-Id") Long employerId) {
        return ResponseEntity.ok(jobCommandService.activateJob(jobId, employerId));
    }

    @PutMapping("/{jobId}/pause")
    public ResponseEntity<JobResponse> pauseJob(
            @PathVariable Long jobId,
            @RequestHeader("X-User-Id") Long employerId) {
        return ResponseEntity.ok(jobCommandService.pauseJob(jobId, employerId));
    }

    @PutMapping("/{jobId}/close")
    public ResponseEntity<JobResponse> closeJob(
            @PathVariable Long jobId,
            @RequestHeader("X-User-Id") Long employerId) {
        return ResponseEntity.ok(jobCommandService.closeJob(jobId, employerId));
    }

    // ==================== Delete ====================

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long jobId,
            @RequestHeader("X-User-Id") Long employerId) {
        jobCommandService.deleteJob(jobId, employerId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Company Endpoints ====================

    @PostMapping("/companies")
    public ResponseEntity<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyRequest request,
            @RequestHeader("X-User-Id") Long employerId) {
        CompanyResponse company = jobCommandService.createCompany(request, employerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @GetMapping("/companies")
    public ResponseEntity<java.util.List<CompanyResponse>> getAllCompanies() {
        java.util.List<CompanyResponse> companies = jobCommandService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/companies/mine")
    public ResponseEntity<java.util.List<CompanyResponse>> getMyCompanies(
            @RequestHeader("X-User-Id") Long employerId) {
        java.util.List<CompanyResponse> companies = jobCommandService.getCompaniesByUser(employerId);
        return ResponseEntity.ok(companies);
    }
}
