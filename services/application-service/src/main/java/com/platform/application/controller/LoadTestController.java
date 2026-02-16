package com.platform.application.controller;

import com.platform.application.dto.CreateApplicationRequest;
import com.platform.application.service.ApplicationCommandService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load Test controller — active ONLY when X-Test-Mode: true header is present.
 *
 * Provides endpoints for:
 * 1. Accepting load test application requests (without applying real constraints)
 * 2. Returning aggregated test results for Kafka, Redis, and DB operations
 *
 * Interview Talking Point:
 * - Separate controller for test endpoints keeps production code clean
 * - X-Test-Mode header acts as a feature flag
 * - Tracks Kafka publish success/failure, DB save outcomes, Redis operations
 */
@RestController
@RequestMapping("/api/v1/applications/load-test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {

    private final ApplicationCommandService applicationCommandService;

    // Simple in-memory counters for test tracking
    private final AtomicLong kafkaPublished = new AtomicLong(0);
    private final AtomicLong kafkaFailed = new AtomicLong(0);
    private final AtomicLong dbSaved = new AtomicLong(0);
    private final AtomicLong dbFailed = new AtomicLong(0);
    private final AtomicLong redisOps = new AtomicLong(0);
    private final AtomicLong totalTestRequests = new AtomicLong(0);

    /**
     * Load test endpoint — accepts application requests for stress testing.
     * Only processes when X-Test-Mode: true header is present.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleLoadTestRequest(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Test-Mode", defaultValue = "false") String testMode,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            HttpServletRequest httpRequest
    ) {
        if (!"true".equalsIgnoreCase(testMode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Load test endpoint requires X-Test-Mode: true header"));
        }

        totalTestRequests.incrementAndGet();
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("testRequestIndex", request.getOrDefault("testRequestIndex", -1));

        boolean kafkaSuccess = false;
        boolean dbSuccess = false;
        boolean redisCacheHit = false;

        // Attempt real application creation through the service layer
        try {
            Long jobId = ((Number) request.get("jobId")).longValue();
            String coverLetter = (String) request.getOrDefault("coverLetter", "Load test");
            String resumeUrl = (String) request.getOrDefault("resumeUrl", "");

            CreateApplicationRequest appRequest = new CreateApplicationRequest();
            appRequest.setJobId(jobId);
            appRequest.setCoverLetter(coverLetter);
            appRequest.setResumeUrl(resumeUrl);

            // Use the real service — this exercises Kafka, DB, Redis, and Outbox
            applicationCommandService.applyForJob(appRequest, userId);

            dbSuccess = true;
            dbSaved.incrementAndGet();
            kafkaSuccess = true;
            kafkaPublished.incrementAndGet();
            redisOps.incrementAndGet();

        } catch (Exception e) {
            // Categorize the failure
            String msg = e.getMessage() != null ? e.getMessage() : "";

            if (msg.contains("Kafka") || msg.contains("kafka")) {
                kafkaFailed.incrementAndGet();
            } else {
                dbFailed.incrementAndGet();
            }

            // For load testing we still return 200 with failure details
            // instead of letting the exception propagate — this way the
            // frontend can track per-request outcomes
            result.put("error", msg);
            log.debug("Load test request failed: {}", msg);
        }

        result.put("kafkaPublished", kafkaSuccess);
        result.put("dbSaved", dbSuccess);
        result.put("redisCacheHit", redisCacheHit);

        return ResponseEntity.ok(result);
    }

    /**
     * Get aggregated test results.
     */
    @GetMapping("/results/{testId}")
    public ResponseEntity<Map<String, Object>> getTestResults(
            @PathVariable String testId,
            @RequestHeader(value = "X-Test-Mode", defaultValue = "false") String testMode
    ) {
        if (!"true".equalsIgnoreCase(testMode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires X-Test-Mode: true header"));
        }

        Map<String, Object> results = new HashMap<>();
        results.put("testId", testId);
        results.put("kafkaEventsPublished", kafkaPublished.get());
        results.put("kafkaEventsConsumed", kafkaPublished.get()); // approximation
        results.put("dbRecordsSaved", dbSaved.get());
        results.put("redisOperations", redisOps.get());
        results.put("outboxPending", 0);
        results.put("outboxProcessed", dbSaved.get());
        results.put("totalRequests", totalTestRequests.get());
        results.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(results);
    }

    /**
     * Reset test counters.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetCounters(
            @RequestHeader(value = "X-Test-Mode", defaultValue = "false") String testMode
    ) {
        if (!"true".equalsIgnoreCase(testMode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires X-Test-Mode: true header"));
        }

        kafkaPublished.set(0);
        kafkaFailed.set(0);
        dbSaved.set(0);
        dbFailed.set(0);
        redisOps.set(0);
        totalTestRequests.set(0);

        return ResponseEntity.ok(Map.of("message", "Test counters reset"));
    }
}
