package com.platform.job.repository;

import com.platform.job.model.Job;
import com.platform.job.model.Job.ExperienceLevel;
import com.platform.job.model.Job.JobStatus;
import com.platform.job.model.Job.JobType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic query specifications for Job search.
 * Uses JPA Criteria API to build queries that leverage the composite index.
 *
 * Interview Talking Point:
 * - Specification pattern allows composable, type-safe queries
 * - Column order in predicates matches idx_job_search (status, role, location, created_at)
 * - MySQL optimizer can use leftmost prefix of composite index
 */
public final class JobSpecifications {

    private JobSpecifications() {
        // Utility class
    }

    public static Specification<Job> buildSearchSpec(
            String keyword,
            String location,
            String role,
            JobType jobType,
            ExperienceLevel experienceLevel,
            JobStatus status,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            Long companyId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter (first column in composite index)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // Default to ACTIVE for public-facing searches
                predicates.add(cb.equal(root.get("status"), JobStatus.ACTIVE));
            }

            // Role filter (second column in composite index)
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("role")), role.toLowerCase()));
            }

            // Location filter (third column in composite index)
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")),
                        "%" + location.toLowerCase() + "%"));
            }

            // Keyword search (not in composite index — scans after index filter)
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate roleMatch = cb.like(cb.lower(root.get("role")), pattern);
                predicates.add(cb.or(titleMatch, descMatch, roleMatch));
            }

            // Job type filter
            if (jobType != null) {
                predicates.add(cb.equal(root.get("jobType"), jobType));
            }

            // Experience level filter
            if (experienceLevel != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            }

            // Salary range filter
            if (salaryMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryMin));
            }
            if (salaryMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), salaryMax));
            }

            // Company filter
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }

            // Default sort by created_at DESC (fourth column in composite index)
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
