package com.platform.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Notification message formatting utility.
 * 
 * Responsibilities:
 *   - Format SMS messages (character-limited)
 *   - Format push notification titles/bodies
 *   - Resolve user contact info (placeholder for cross-service calls)
 *   - Build fallback plain-text content
 * 
 * In production: User/Job lookup would be via:
 *   - REST calls to User Service / Job Service (with circuit breaker)
 *   - Or a local read-through cache populated from Kafka events
 *   - Or event payload enrichment at the producer side
 */
@Component
@Slf4j
public class NotificationFormatter {

    /**
     * Resolve user email address by userId.
     * In production: REST call to User Service with caching.
     */
    public String resolveUserEmail(Long userId) {
        // Placeholder — in production, call User Service or cache lookup
        return "user" + userId + "@jobplatform.com";
    }

    /**
     * Resolve user phone number by userId.
     * Returns null if user hasn't registered a phone number.
     */
    public String resolveUserPhone(Long userId) {
        // Placeholder — in production, call User Service
        return null; // Most users won't have phone registered
    }

    /**
     * Resolve job title by jobId.
     * In production: REST call to Job Service with caching.
     */
    public String resolveJobTitle(Long jobId) {
        // Placeholder — in production, call Job Service or cache lookup
        return "Job #" + jobId;
    }

    /**
     * Format SMS message (max 160 chars for single SMS segment).
     */
    public String formatSmsMessage(String eventType, String status, Map<String, Object> event) {
        String safeEventType = eventType != null ? eventType : "UNKNOWN";
        return switch (safeEventType) {
            case "APPLICATION_STATUS_CHANGED" ->
                    String.format("Job Platform: Your application status changed to %s. Check your email for details.", status);
            case "APPLICATION_CREATED" ->
                    "Job Platform: Application submitted successfully! Check your email for confirmation.";
            default ->
                    "Job Platform: You have a new notification. Check your email for details.";
        };
    }

    /**
     * Format push notification title.
     */
    public String formatPushTitle(String eventType) {
        String safeEventType = eventType != null ? eventType : "UNKNOWN";
        return switch (safeEventType) {
            case "APPLICATION_CREATED" -> "Application Submitted! 🎉";
            case "APPLICATION_STATUS_CHANGED" -> "Status Update 📋";
            case "APPLICATION_WITHDRAWN" -> "Application Withdrawn";
            default -> "Job Platform Update";
        };
    }

    /**
     * Format push notification body.
     */
    public String formatPushBody(String eventType, Map<String, Object> event) {
        String status = String.valueOf(event.getOrDefault("status", ""));
        String safeEventType = eventType != null ? eventType : "UNKNOWN";
        return switch (safeEventType) {
            case "APPLICATION_CREATED" ->
                    "Your application has been submitted successfully.";
            case "APPLICATION_STATUS_CHANGED" ->
                    "Your application status has been updated to: " + status;
            case "APPLICATION_WITHDRAWN" ->
                    "Your application has been withdrawn.";
            default ->
                    "You have a new notification from Job Platform.";
        };
    }

    /**
     * Build fallback plain text for retry scenarios (when template rendering fails).
     */
    public String formatFallbackText(Map<String, Object> event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Job Platform Notification\n\n");

        String eventType = String.valueOf(event.getOrDefault("eventType", "UPDATE"));
        sb.append("Type: ").append(eventType).append("\n");

        Object jobId = event.get("jobId");
        if (jobId != null) {
            sb.append("Job ID: ").append(jobId).append("\n");
        }

        Object status = event.get("status");
        if (status != null) {
            sb.append("Status: ").append(status).append("\n");
        }

        sb.append("\nPlease visit the platform for more details.");
        return sb.toString();
    }
}
