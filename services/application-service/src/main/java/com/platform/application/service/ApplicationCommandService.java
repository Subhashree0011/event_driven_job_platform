package com.platform.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.application.dto.*;
import com.platform.application.exception.ApplicationException;
import com.platform.application.model.Application;
import com.platform.application.model.Application.ApplicationStatus;
import com.platform.application.outbox.OutboxEvent;
import com.platform.application.repository.ApplicationRepository;
import com.platform.application.repository.OutboxEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Command side — handles all application mutations.
 * Uses the Transactional Outbox Pattern to ensure events are published reliably.
 *
 * Interview Talking Point:
 * - Application write + OutboxEvent write happen in the SAME transaction
 * - If transaction commits → event is guaranteed to be published (eventually)
 * - If transaction rolls back → no orphan event in Kafka
 * - This solves the dual-write problem (DB + Kafka atomicity)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationCommandService {

    private final ApplicationRepository applicationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationValidationService validationService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Submit a new job application.
     * Creates the application AND writes an outbox event in the same transaction.
     */
    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker")
    public ApplicationResponse applyForJob(CreateApplicationRequest request, Long userId) {
        // 1. Validate — no duplicate applications
        validationService.validateNewApplication(userId, request.getJobId());

        // 2. Create application entity
        Application application = ApplicationMapper.toEntity(request, userId);
        @SuppressWarnings("null")
        Application savedApp = Objects.requireNonNull(applicationRepository.save(application),
                "Application save returned null");
        application = savedApp;

        log.info("Application created: id={}, user={}, job={}",
                application.getId(), userId, request.getJobId());

        // 3. Write outbox event in the same transaction
        ApplicationEvent event = ApplicationEvent.created(
                application.getId(), request.getJobId(), userId
        );
        writeOutboxEvent(application, event, "APPLICATION_CREATED", "application.created");

        meterRegistry.counter("application.created").increment();
        return ApplicationMapper.toResponse(application);
    }

    /**
     * Update application status (by employer).
     * Validates the state machine transition before applying.
     */
    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker")
    public ApplicationResponse updateStatus(Long applicationId, UpdateStatusRequest request) {
        Application application = applicationRepository.findById(
                Objects.requireNonNull(applicationId, "applicationId must not be null"))
                .orElseThrow(() -> new ApplicationException(
                        "APPLICATION_NOT_FOUND",
                        "Application not found: " + applicationId,
                        HttpStatus.NOT_FOUND
                ));

        // Validate state transition
        validationService.validateStatusTransition(application, request.getStatus());

        application.transitionTo(request.getStatus());
        if (request.getNotes() != null) {
            application.setNotes(request.getNotes());
        }
        application = applicationRepository.save(application);

        log.info("Application {} status changed to {}", applicationId, request.getStatus());

        // Write outbox event
        ApplicationEvent event = ApplicationEvent.statusChanged(
                application.getId(), application.getJobId(),
                application.getUserId(), application.getStatus().name()
        );
        writeOutboxEvent(application, event, "APPLICATION_STATUS_CHANGED", "application.created");

        meterRegistry.counter("application.status_changed",
                "newStatus", request.getStatus().name()).increment();
        return ApplicationMapper.toResponse(application);
    }

    /**
     * Withdraw an application (by applicant).
     */
    @Transactional
    @CircuitBreaker(name = "databaseCircuitBreaker")
    public ApplicationResponse withdrawApplication(Long applicationId, Long userId) {
        Application application = applicationRepository.findById(
                Objects.requireNonNull(applicationId, "applicationId must not be null"))
                .orElseThrow(() -> new ApplicationException(
                        "APPLICATION_NOT_FOUND",
                        "Application not found: " + applicationId,
                        HttpStatus.NOT_FOUND
                ));

        // Validate ownership and state
        validationService.validateOwnership(application, userId);
        validationService.validateStatusTransition(application, ApplicationStatus.WITHDRAWN);

        application.transitionTo(ApplicationStatus.WITHDRAWN);
        application = applicationRepository.save(application);

        log.info("Application {} withdrawn by user {}", applicationId, userId);

        // Write outbox event
        ApplicationEvent event = ApplicationEvent.withdrawn(
                application.getId(), application.getJobId(), userId
        );
        writeOutboxEvent(application, event, "APPLICATION_WITHDRAWN", "application.created");

        meterRegistry.counter("application.withdrawn").increment();
        return ApplicationMapper.toResponse(application);
    }

    // === Read Operations ===

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long applicationId) {
        Application application = applicationRepository.findById(
                Objects.requireNonNull(applicationId, "applicationId must not be null"))
                .orElseThrow(() -> new ApplicationException(
                        "APPLICATION_NOT_FOUND",
                        "Application not found: " + applicationId,
                        HttpStatus.NOT_FOUND
                ));
        return ApplicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByUser(Long userId, int page, int size) {
        return applicationRepository.findByUserId(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ApplicationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByJob(Long jobId, int page, int size) {
        return applicationRepository.findByJobId(jobId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ApplicationMapper::toResponse);
    }

    // === Private Helpers ===

    /**
     * Write an outbox event in the current transaction.
     * The event will be picked up by OutboxPoller and published to Kafka.
     */
    private void writeOutboxEvent(Application application, ApplicationEvent event,
                                   String eventType, String topic) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Application")
                    .aggregateId(application.getId())
                    .eventType(eventType)
                    .payload(payload)
                    .topic(topic)
                    .partitionKey(String.valueOf(application.getJobId())) // partition by job for ordering
                    .build();

            outboxEventRepository.save(Objects.requireNonNull(outboxEvent, "OutboxEvent must not be null"));
            log.debug("Outbox event written: type={}, aggregateId={}", eventType, application.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}
