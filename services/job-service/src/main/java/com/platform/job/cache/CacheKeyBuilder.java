package com.platform.job.cache;

/**
 * Builds deterministic, human-readable cache keys.
 * Consistent key structure prevents cache pollution and simplifies debugging.
 */
public final class CacheKeyBuilder {

    private static final String SEPARATOR = ":";

    private CacheKeyBuilder() {
        // Utility class
    }

    public static String jobDetail(Long jobId) {
        return "job" + SEPARATOR + "detail" + SEPARATOR + jobId;
    }

    public static String jobSearch(String queryHash) {
        return "job" + SEPARATOR + "search" + SEPARATOR + queryHash;
    }

    public static String companyJobs(Long companyId) {
        return "job" + SEPARATOR + "company" + SEPARATOR + companyId;
    }

    public static String employerJobs(Long employerId) {
        return "job" + SEPARATOR + "employer" + SEPARATOR + employerId;
    }

    public static String jobViewCount(Long jobId) {
        return "job" + SEPARATOR + "views" + SEPARATOR + jobId;
    }
}
