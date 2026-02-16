package com.platform.job.dto;

import com.platform.job.model.Job;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    private String requirements;

    @NotBlank(message = "Role is required")
    @Size(max = 100, message = "Role must not exceed 100 characters")
    private String role;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;

    @DecimalMin(value = "0.0", message = "Minimum salary must be positive")
    private BigDecimal salaryMin;

    @DecimalMin(value = "0.0", message = "Maximum salary must be positive")
    private BigDecimal salaryMax;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency;

    @Future(message = "Application deadline must be in the future")
    private LocalDate applicationDeadline;

    @Size(max = 20, message = "Maximum 20 skills allowed")
    private Set<String> skills;
}
