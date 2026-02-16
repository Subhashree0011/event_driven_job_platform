package com.platform.job.service;

import com.platform.job.dto.*;
import com.platform.job.exception.JobNotFoundException;
import com.platform.job.messaging.producer.JobEventProducer;
import com.platform.job.model.Company;
import com.platform.job.model.Job;
import com.platform.job.repository.CompanyRepository;
import com.platform.job.repository.JobRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Write-side of CQRS — handles all job mutations.
 * Every mutation invalidates the relevant cache entries
 * and publishes Kafka events for downstream services.
 *
 * Interview Talking Point:
 * - CQRS allows different optimization strategies for reads vs writes
 * - Writes go through validation, cache invalidation, event publishing
 * - Scheduled job expiration runs in this service (single writer principle)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCommandService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobEventProducer jobEventProducer;
    private final CacheAwareJobService cacheAwareJobService;
    private final MeterRegistry meterRegistry;

    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker")
    public JobResponse createJob(CreateJobRequest request, Long employerId) {
        Company company = companyRepository.findById(
                Objects.requireNonNull(request.getCompanyId(), "companyId must not be null"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Company not found with id: " + request.getCompanyId()));

        // Validate salary range
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        }

        Job job = JobMapper.toJob(request, company, employerId);
        @SuppressWarnings("null")
        Job savedJob = Objects.requireNonNull(jobRepository.save(job), "Job save returned null");
        job = savedJob;

        log.info("Job created: id={}, title={}, employer={}", job.getId(), job.getTitle(), employerId);
        meterRegistry.counter("job.created", "jobType", job.getJobType().name()).increment();

        // Publish event to Kafka for analytics/notification
        jobEventProducer.publishJobCreated(JobEvent.created(
                job.getId(), company.getId(), employerId,
                job.getTitle(), job.getRole(), job.getLocation()));

        // Invalidate search cache (new job should appear in search results)
        cacheAwareJobService.invalidateSearchCache();

        return JobMapper.toJobResponse(job);
    }

    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker")
    public JobResponse updateJob(Long jobId, UpdateJobRequest request, Long employerId) {
        Job job = jobRepository.findById(Objects.requireNonNull(jobId, "jobId must not be null"))
                .orElseThrow(() -> new JobNotFoundException(jobId));

        // Authorization check: only the employer who created the job can update it
        if (!job.getEmployerId().equals(employerId)) {
            throw new IllegalArgumentException("You are not authorized to update this job");
        }

        // Validate salary range if both provided
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        }

        JobMapper.updateJob(job, request);
        job = jobRepository.save(job);

        log.info("Job updated: id={}, employer={}", jobId, employerId);

        // Publish update event
        jobEventProducer.publishJobUpdated(JobEvent.updated(
                job.getId(), job.getTitle(), job.getRole(), job.getLocation()));

        // Invalidate caches
        cacheAwareJobService.evictJobDetail(jobId);
        cacheAwareJobService.invalidateSearchCache();

        return JobMapper.toJobResponse(job);
    }

    @Transactional
    public JobResponse activateJob(Long jobId, Long employerId) {
        return changeJobStatus(jobId, employerId, Job.JobStatus.ACTIVE, "JOB_ACTIVATED");
    }

    @Transactional
    public JobResponse pauseJob(Long jobId, Long employerId) {
        return changeJobStatus(jobId, employerId, Job.JobStatus.PAUSED, "JOB_PAUSED");
    }

    @Transactional
    public JobResponse closeJob(Long jobId, Long employerId) {
        return changeJobStatus(jobId, employerId, Job.JobStatus.CLOSED, "JOB_CLOSED");
    }

    @Transactional
    public void deleteJob(Long jobId, Long employerId) {
        Job job = jobRepository.findById(Objects.requireNonNull(jobId, "jobId must not be null"))
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getEmployerId().equals(employerId)) {
            throw new IllegalArgumentException("You are not authorized to delete this job");
        }

        jobRepository.delete(job);
        log.info("Job deleted: id={}, employer={}", jobId, employerId);
        meterRegistry.counter("job.deleted").increment();

        cacheAwareJobService.evictJobDetail(jobId);
        cacheAwareJobService.invalidateSearchCache();
    }

    // === Company Operations ===

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request, Long employerId) {
        if (companyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Company already exists: " + request.getName());
        }

        Company company = JobMapper.toCompany(request);
        company.setCreatedBy(employerId);
        @SuppressWarnings("null")
        Company savedCompany = Objects.requireNonNull(companyRepository.save(company), "Company save returned null");
        company = savedCompany;

        log.info("Company created: id={}, name={}, createdBy={}", company.getId(), company.getName(), employerId);
        meterRegistry.counter("company.created").increment();

        return JobMapper.toCompanyResponse(company);
    }

    @Transactional(readOnly = true)
    public java.util.List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(JobMapper::toCompanyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<CompanyResponse> getCompaniesByUser(Long userId) {
        return companyRepository.findByCreatedBy(userId).stream()
                .map(JobMapper::toCompanyResponse)
                .toList();
    }

    // === Scheduled Job Expiration ===

    /**
     * Runs every hour — expires jobs past their application deadline.
     * Single writer pattern ensures no concurrent expiration conflicts.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOverdueJobs() {
        int expired = jobRepository.expireJobsPastDeadline(LocalDate.now());
        if (expired > 0) {
            log.info("Expired {} jobs past their application deadline", expired);
            meterRegistry.counter("job.expired.batch").increment(expired);
            cacheAwareJobService.invalidateSearchCache();
        }
    }

    // === Private Helpers ===

    private JobResponse changeJobStatus(Long jobId, Long employerId, Job.JobStatus newStatus, String eventType) {
        Job job = jobRepository.findById(Objects.requireNonNull(jobId, "jobId must not be null"))
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getEmployerId().equals(employerId)) {
            throw new IllegalArgumentException("You are not authorized to modify this job");
        }

        job.setStatus(newStatus);
        job = jobRepository.save(job);

        log.info("Job status changed: id={}, status={}, employer={}", jobId, newStatus, employerId);
        meterRegistry.counter("job.status.changed", "newStatus", newStatus.name()).increment();

        // Publish status change event
        jobEventProducer.publishJobStatusChanged(JobEvent.statusChanged(jobId, newStatus.name()));

        // Invalidate caches
        cacheAwareJobService.evictJobDetail(jobId);
        cacheAwareJobService.invalidateSearchCache();

        return JobMapper.toJobResponse(job);
    }
}
