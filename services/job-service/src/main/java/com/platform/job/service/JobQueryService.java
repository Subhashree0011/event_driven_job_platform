package com.platform.job.service;

import com.platform.job.dto.JobMapper;
import com.platform.job.dto.JobResponse;
import com.platform.job.dto.JobSearchRequest;
import com.platform.job.model.Job;
import com.platform.job.repository.JobRepository;
import com.platform.job.repository.JobSpecifications;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Read-side of CQRS — handles all job queries.
 * No state mutation, all methods are @Transactional(readOnly = true).
 *
 * Interview Talking Point:
 * - Read-only transactions skip dirty checking and flush (performance win)
 * - MySQL can route to read replicas with readOnly hint
 * - Separating reads from writes allows independent scaling
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JobQueryService {

    private final JobRepository jobRepository;
    private final MeterRegistry meterRegistry;

    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "searchFallback")
    public Page<JobResponse> searchJobs(JobSearchRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Pageable pageable = buildPageable(request);

            Specification<Job> spec = Objects.requireNonNull(
                    JobSpecifications.buildSearchSpec(
                            request.getKeyword(),
                            request.getLocation(),
                            request.getRole(),
                            request.getJobType(),
                            request.getExperienceLevel(),
                            request.getStatus(),
                            request.getSalaryMin(),
                            request.getSalaryMax(),
                            request.getCompanyId()
                    ), "Search specification must not be null");

            Page<Job> jobs = jobRepository.findAll(
                    spec,
                    Objects.requireNonNull(pageable, "Pageable must not be null")
            );

            log.debug("Search returned {} jobs (page {}/{})",
                    jobs.getNumberOfElements(), request.getPage(), jobs.getTotalPages());

            return jobs.map(JobMapper::toJobResponse);
        } finally {
            sample.stop(Timer.builder("job.search.duration")
                    .tag("source", "database")
                    .register(meterRegistry));
        }
    }

    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "getJobFallback")
    public JobResponse getJobById(Long jobId) {
        Job job = jobRepository.findById(Objects.requireNonNull(jobId, "jobId must not be null"))
                .orElseThrow(() -> new com.platform.job.exception.JobNotFoundException(jobId));

        return JobMapper.toJobResponse(job);
    }

    public Page<JobResponse> getJobsByEmployer(Long employerId, int page, int size) {
        Page<Job> jobs = jobRepository.findByEmployerId(employerId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return jobs.map(JobMapper::toJobResponse);
    }

    public Page<JobResponse> getJobsByCompany(Long companyId, int page, int size) {
        Page<Job> jobs = jobRepository.findByCompanyId(companyId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return jobs.map(JobMapper::toJobResponse);
    }

    @Transactional
    public void recordView(Long jobId) {
        jobRepository.incrementViewCount(jobId);
        meterRegistry.counter("job.views", "jobId", jobId.toString()).increment();
    }

    // === Pagination Helper ===

    private Pageable buildPageable(JobSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // default

        if (request.getSortBy() != null) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            sort = switch (request.getSortBy().toLowerCase()) {
                case "salary" -> Sort.by(direction, "salaryMax");
                case "viewcount", "views" -> Sort.by(direction, "viewCount");
                case "createdat", "date" -> Sort.by(direction, "createdAt");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
        }

        return PageRequest.of(
                Math.max(0, request.getPage()),
                Math.min(Math.max(1, request.getSize()), 100), // cap at 100
                sort
        );
    }

    // === Circuit Breaker Fallbacks ===

    @SuppressWarnings("unused")
    private Page<JobResponse> searchFallback(JobSearchRequest request, Throwable t) {
        log.error("Database circuit breaker open for job search: {}", t.getMessage());
        meterRegistry.counter("job.search.fallback").increment();
        return Page.empty();
    }

    @SuppressWarnings("unused")
    private JobResponse getJobFallback(Long jobId, Throwable t) {
        log.error("Database circuit breaker open for job fetch {}: {}", jobId, t.getMessage());
        meterRegistry.counter("job.fetch.fallback").increment();
        throw new com.platform.job.exception.FallbackException(
                "Job service is temporarily unavailable. Please try again shortly.");
    }
}
