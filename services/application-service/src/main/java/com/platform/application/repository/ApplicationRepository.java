package com.platform.application.repository;

import com.platform.application.model.Application;
import com.platform.application.model.Application.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * Find application by user and job — checks for duplicate applications.
     * Uses unique constraint uk_application_user_job.
     */
    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    Page<Application> findByUserId(Long userId, Pageable pageable);

    Page<Application> findByJobId(Long jobId, Pageable pageable);

    Page<Application> findByJobIdAndStatus(Long jobId, ApplicationStatus status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobId = :jobId")
    long countByJobId(@Param("jobId") Long jobId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.jobId = :jobId AND a.status = :status")
    long countByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") ApplicationStatus status);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
