package com.platform.job.dto;

import com.platform.job.model.Job;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Search criteria for job listings.
 * All fields are optional — null fields are not included in the query.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSearchRequest {

    private String keyword;               // free-text search in title, description, role
    private String location;
    private String role;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private Job.JobStatus status;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private Set<String> skills;
    private Long companyId;
    private String sortBy;                // "createdAt", "salary", "viewCount"
    private String sortDirection;         // "ASC", "DESC"

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
