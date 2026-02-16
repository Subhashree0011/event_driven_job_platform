package com.platform.job.service;

import com.platform.job.dto.JobResponse;
import com.platform.job.dto.JobSearchRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cache-aside pattern implementation for job searches.
 *
 * Pattern: Check cache → if miss, query DB → store in cache → return.
 * TTL: 60 seconds with ±10 second jitter to prevent thundering herd.
 *
 * Interview Talking Point:
 * - Cache-aside gives the application full control over cache lifecycle
 * - Jitter prevents all cache entries from expiring simultaneously
 * - Stale-while-revalidate: if Redis is down, serve from DB directly
 * - Search results are cached by their query fingerprint (hash of search params)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class CacheAwareJobService {

    private static final String CACHE_PREFIX = "job:search:";
    private static final String DETAIL_PREFIX = "job:detail:";
    private static final long SEARCH_TTL_SECONDS = 60;
    private static final long DETAIL_TTL_SECONDS = 300;
    private static final long TTL_JITTER_SECONDS = 10;

    private final JobQueryService jobQueryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Cache-aside job search.
     * Cache key = hash of search parameters for deterministic lookup.
     */
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "searchWithoutCache")
    public Page<JobResponse> searchJobsCached(JobSearchRequest request) {
        String cacheKey = buildSearchCacheKey(request);
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // 1. Check cache
            @SuppressWarnings("unchecked")
            Page<JobResponse> cached = (Page<JobResponse>) redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Cache HIT for search: {}", cacheKey);
                meterRegistry.counter("job.cache.hit", "type", "search").increment();
                return cached;
            }

            // 2. Cache miss — query database
            log.debug("Cache MISS for search: {}", cacheKey);
            meterRegistry.counter("job.cache.miss", "type", "search").increment();

            Page<JobResponse> results = jobQueryService.searchJobs(request);

            // 3. Store in cache with jittered TTL
            long ttl = SEARCH_TTL_SECONDS + ThreadLocalRandom.current().nextLong(-TTL_JITTER_SECONDS, TTL_JITTER_SECONDS + 1);
            Duration duration = Objects.requireNonNull(Duration.ofSeconds(ttl));
            redisTemplate.opsForValue().set(cacheKey, results, duration);

            return results;
        } finally {
            sample.stop(Timer.builder("job.search.duration")
                    .tag("source", "cache-aside")
                    .register(meterRegistry));
        }
    }

    /**
     * Cache-aside for single job detail.
     * Longer TTL (300s) since individual jobs change less frequently.
     */
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getJobWithoutCache")
    public JobResponse getJobCached(Long jobId) {
        String cacheKey = DETAIL_PREFIX + jobId;

        // 1. Check cache
        JobResponse cached = (JobResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for job: {}", jobId);
            meterRegistry.counter("job.cache.hit", "type", "detail").increment();
            return cached;
        }

        // 2. Cache miss — query database
        log.debug("Cache MISS for job: {}", jobId);
        meterRegistry.counter("job.cache.miss", "type", "detail").increment();

        JobResponse result = jobQueryService.getJobById(jobId);

        // 3. Store in cache
        long ttl = DETAIL_TTL_SECONDS + ThreadLocalRandom.current().nextLong(-TTL_JITTER_SECONDS, TTL_JITTER_SECONDS + 1);
        Duration duration = Objects.requireNonNull(Duration.ofSeconds(ttl));
        redisTemplate.opsForValue().set(cacheKey, result, duration);

        return result;
    }

    // === Cache Invalidation ===

    public void evictJobDetail(Long jobId) {
        String cacheKey = DETAIL_PREFIX + jobId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Evicted cache for job: {}", jobId);
        }
    }

    /**
     * Invalidate all search caches.
     * Uses Redis key pattern scan — acceptable because search cache has short TTL
     * and we only call this on writes (which are infrequent compared to reads).
     */
    public void invalidateSearchCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} search cache entries", keys.size());
                meterRegistry.counter("job.cache.invalidation", "type", "search").increment(keys.size());
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate search cache: {}", e.getMessage());
            // Non-critical — cache will expire naturally via TTL
        }
    }

    // === Circuit Breaker Fallbacks ===

    /**
     * When Redis is down, fall through to database directly.
     * This is the stale-while-revalidate pattern — graceful degradation.
     */
    @SuppressWarnings("unused")
    private Page<JobResponse> searchWithoutCache(JobSearchRequest request, Throwable t) {
        log.warn("Redis circuit breaker open, searching without cache: {}", t.getMessage());
        meterRegistry.counter("job.cache.bypass", "type", "search").increment();
        return jobQueryService.searchJobs(request);
    }

    @SuppressWarnings("unused")
    private JobResponse getJobWithoutCache(Long jobId, Throwable t) {
        log.warn("Redis circuit breaker open, fetching job {} without cache: {}", jobId, t.getMessage());
        meterRegistry.counter("job.cache.bypass", "type", "detail").increment();
        return jobQueryService.getJobById(jobId);
    }

    // === Cache Key Builder ===

    /**
     * Deterministic cache key from search parameters.
     * Uses a stable hash to avoid key explosion.
     */
    private String buildSearchCacheKey(JobSearchRequest request) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX);
        if (request.getKeyword() != null) sb.append("kw:").append(request.getKeyword()).append("|");
        if (request.getLocation() != null) sb.append("loc:").append(request.getLocation()).append("|");
        if (request.getRole() != null) sb.append("role:").append(request.getRole()).append("|");
        if (request.getJobType() != null) sb.append("type:").append(request.getJobType()).append("|");
        if (request.getExperienceLevel() != null) sb.append("exp:").append(request.getExperienceLevel()).append("|");
        if (request.getStatus() != null) sb.append("st:").append(request.getStatus()).append("|");
        if (request.getCompanyId() != null) sb.append("co:").append(request.getCompanyId()).append("|");
        sb.append("p:").append(request.getPage()).append("|s:").append(request.getSize());
        return sb.toString();
    }
}
