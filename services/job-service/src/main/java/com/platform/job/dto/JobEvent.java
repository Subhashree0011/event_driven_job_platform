package com.platform.job.dto;

import lombok.*;

import java.util.Map;

/**
 * Kafka event payload for job lifecycle events.
 * Published when jobs are created, updated, activated, paused, or closed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEvent {

    private String eventType;      // JOB_CREATED, JOB_UPDATED, JOB_ACTIVATED, JOB_CLOSED, etc.
    private Long jobId;
    private Long companyId;
    private Long employerId;
    private String title;
    private String role;
    private String location;
    private String status;
    private Long timestamp;
    private Map<String, Object> metadata;

    public static JobEvent created(Long jobId, Long companyId, Long employerId, String title, String role, String location) {
        return JobEvent.builder()
                .eventType("JOB_CREATED")
                .jobId(jobId)
                .companyId(companyId)
                .employerId(employerId)
                .title(title)
                .role(role)
                .location(location)
                .status("DRAFT")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static JobEvent statusChanged(Long jobId, String newStatus) {
        return JobEvent.builder()
                .eventType("JOB_STATUS_CHANGED")
                .jobId(jobId)
                .status(newStatus)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static JobEvent updated(Long jobId, String title, String role, String location) {
        return JobEvent.builder()
                .eventType("JOB_UPDATED")
                .jobId(jobId)
                .title(title)
                .role(role)
                .location(location)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
