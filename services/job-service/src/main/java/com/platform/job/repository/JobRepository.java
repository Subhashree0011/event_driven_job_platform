package com.platform.job.repository;

import com.platform.job.model.Job;
import com.platform.job.model.Job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Job repository with JpaSpecificationExecutor for dynamic search queries.
 * Uses the composite index idx_job_search (status, role, location, created_at DESC)
 * for optimized search performance.
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    // === Basic Queries ===

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByEmployerId(Long employerId, Pageable pageable);

    Page<Job> findByCompanyId(Long companyId, Pageable pageable);

    // === Search-optimized queries leveraging composite index ===

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.role = :role AND j.location = :location ORDER BY j.createdAt DESC")
    Page<Job> findByStatusAndRoleAndLocation(
            @Param("status") JobStatus status,
            @Param("role") String role,
            @Param("location") String location,
            Pageable pageable
    );

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.role) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // === Count queries for analytics ===

    long countByStatus(JobStatus status);

    long countByCompanyId(Long companyId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'ACTIVE' AND j.createdAt >= CURRENT_DATE")
    long countActiveJobsCreatedToday();

    // === Bulk operations ===

    @Modifying
    @Query("UPDATE Job j SET j.status = 'EXPIRED' WHERE j.status = 'ACTIVE' AND j.applicationDeadline < :today")
    int expireJobsPastDeadline(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE Job j SET j.viewCount = j.viewCount + 1 WHERE j.id = :jobId")
    void incrementViewCount(@Param("jobId") Long jobId);

    @Modifying
    @Query("UPDATE Job j SET j.applicationCount = j.applicationCount + 1 WHERE j.id = :jobId")
    void incrementApplicationCount(@Param("jobId") Long jobId);

    // === Skills-based query ===

    @Query("SELECT DISTINCT j FROM Job j JOIN j.skills s WHERE s IN :skills AND j.status = 'ACTIVE'")
    Page<Job> findActiveJobsBySkills(@Param("skills") List<String> skills, Pageable pageable);
}
