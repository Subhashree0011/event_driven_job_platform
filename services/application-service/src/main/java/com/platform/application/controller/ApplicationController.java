package com.platform.application.controller;

import com.platform.application.dto.*;
import com.platform.application.resilience.ApplicationRateLimiter;
import com.platform.application.service.ApplicationCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for job application operations.
 *
 * Interview Talking Point:
 * - User ID comes from X-User-Id header (set by API Gateway after JWT validation)
 * - All state-changing operations go through the Transactional Outbox Pattern
 * - Application follows a state machine: SUBMITTED → UNDER_REVIEW → ... → OFFERED/REJECTED
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationCommandService applicationCommandService;
    private final ApplicationRateLimiter applicationRateLimiter;

    // ==================== Apply for a Job ====================

    @PostMapping
    public ResponseEntity<ApplicationResponse> apply(
            @Valid @RequestBody CreateApplicationRequest request,
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest
    ) {
        // Rate limit check — bypassed in test mode (X-Test-Mode: true)
        applicationRateLimiter.checkApplyRateLimit(httpRequest, userId);

        ApplicationResponse response = applicationCommandService.applyForJob(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== Get Applications ====================

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplication(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(applicationCommandService.getApplication(applicationId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ApplicationResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(applicationCommandService.getApplicationsByUser(userId, page, size));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<ApplicationResponse>> getByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(applicationCommandService.getApplicationsByJob(jobId, page, size));
    }

    // ==================== Status Management ====================

    /**
     * Update application status (by employer/admin).
     */
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(applicationCommandService.updateStatus(applicationId, request));
    }

    /**
     * Withdraw application (by applicant).
     */
    @PutMapping("/{applicationId}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(
            @PathVariable Long applicationId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(applicationCommandService.withdrawApplication(applicationId, userId));
    }
}
