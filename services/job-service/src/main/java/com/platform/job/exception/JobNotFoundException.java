package com.platform.job.exception;

/**
 * Thrown when a job is not found by ID.
 */
public class JobNotFoundException extends RuntimeException {

    private final Long jobId;

    public JobNotFoundException(Long jobId) {
        super("Job not found with id: " + jobId);
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }
}
