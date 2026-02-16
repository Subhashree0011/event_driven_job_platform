package com.platform.job.dto;

import com.platform.job.model.Job;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private Long id;
    private CompanyResponse company;
    private Long employerId;
    private String title;
    private String description;
    private String requirements;
    private String role;
    private String location;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private Job.JobStatus status;
    private LocalDate applicationDeadline;
    private Integer viewCount;
    private Integer applicationCount;
    private Set<String> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
