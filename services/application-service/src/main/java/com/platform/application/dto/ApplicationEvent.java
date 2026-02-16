package com.platform.application.dto;

import lombok.*;

import java.util.Map;

/**
 * Event payload published to Kafka when an application is created or updated.
 * Used by notification-service and analytics-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationEvent {

    private String eventType;
    private Long applicationId;
    private Long jobId;
    private Long userId;
    private String status;
    private Long timestamp;
    private Map<String, Object> metadata;

    public static ApplicationEvent created(Long applicationId, Long jobId, Long userId) {
        return ApplicationEvent.builder()
                .eventType("APPLICATION_CREATED")
                .applicationId(applicationId)
                .jobId(jobId)
                .userId(userId)
                .status("SUBMITTED")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ApplicationEvent statusChanged(Long applicationId, Long jobId, Long userId, String newStatus) {
        return ApplicationEvent.builder()
                .eventType("APPLICATION_STATUS_CHANGED")
                .applicationId(applicationId)
                .jobId(jobId)
                .userId(userId)
                .status(newStatus)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ApplicationEvent withdrawn(Long applicationId, Long jobId, Long userId) {
        return ApplicationEvent.builder()
                .eventType("APPLICATION_WITHDRAWN")
                .applicationId(applicationId)
                .jobId(jobId)
                .userId(userId)
                .status("WITHDRAWN")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
