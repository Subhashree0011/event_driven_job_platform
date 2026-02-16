package com.platform.job.dto;

import com.platform.job.model.Company;
import com.platform.job.model.Job;

/**
 * Maps between entities and DTOs.
 * Stateless utility — no Spring dependency injection needed.
 */
public final class JobMapper {

    private JobMapper() {
        // Utility class — prevent instantiation
    }

    // === Job mappings ===

    public static JobResponse toJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .company(toCompanyResponse(job.getCompany()))
                .employerId(job.getEmployerId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .role(job.getRole())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .status(job.getStatus())
                .applicationDeadline(job.getApplicationDeadline())
                .viewCount(job.getViewCount())
                .applicationCount(job.getApplicationCount())
                .skills(job.getSkills())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public static Job toJob(CreateJobRequest request, Company company, Long employerId) {
        return Job.builder()
                .company(company)
                .employerId(employerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .role(request.getRole())
                .location(request.getLocation())
                .jobType(request.getJobType() != null ? request.getJobType() : Job.JobType.FULL_TIME)
                .experienceLevel(
                        request.getExperienceLevel() != null ? request.getExperienceLevel() : Job.ExperienceLevel.MID)
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .applicationDeadline(request.getApplicationDeadline())
                .skills(request.getSkills() != null ? request.getSkills() : new java.util.HashSet<>())
                .build();
    }

    public static void updateJob(Job job, UpdateJobRequest request) {
        if (request.getTitle() != null)
            job.setTitle(request.getTitle());
        if (request.getDescription() != null)
            job.setDescription(request.getDescription());
        if (request.getRequirements() != null)
            job.setRequirements(request.getRequirements());
        if (request.getRole() != null)
            job.setRole(request.getRole());
        if (request.getLocation() != null)
            job.setLocation(request.getLocation());
        if (request.getJobType() != null)
            job.setJobType(request.getJobType());
        if (request.getExperienceLevel() != null)
            job.setExperienceLevel(request.getExperienceLevel());
        if (request.getSalaryMin() != null)
            job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null)
            job.setSalaryMax(request.getSalaryMax());
        if (request.getCurrency() != null)
            job.setCurrency(request.getCurrency());
        if (request.getApplicationDeadline() != null)
            job.setApplicationDeadline(request.getApplicationDeadline());
        if (request.getSkills() != null)
            job.setSkills(request.getSkills());
    }

    // === Company mappings ===

    public static CompanyResponse toCompanyResponse(Company company) {
        if (company == null)
            return null;
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .description(company.getDescription())
                .website(company.getWebsite())
                .logoUrl(company.getLogoUrl())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .location(company.getLocation())
                .createdBy(company.getCreatedBy())
                .createdAt(company.getCreatedAt())
                .build();
    }

    public static Company toCompany(CompanyRequest request) {
        return Company.builder()
                .name(request.getName())
                .description(request.getDescription())
                .website(request.getWebsite())
                .logoUrl(request.getLogoUrl())
                .industry(request.getIndustry())
                .companySize(request.getCompanySize() != null ? request.getCompanySize() : Company.CompanySize.MEDIUM)
                .location(request.getLocation())
                .build();
    }
}
