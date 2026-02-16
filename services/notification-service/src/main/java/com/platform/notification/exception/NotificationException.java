package com.platform.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Domain exception for notification delivery failures.
 * 
 * Carries:
 *   - errorCode: Machine-readable code for monitoring/alerting
 *   - channel: Which delivery channel failed (EMAIL, SMS, PUSH)
 *   - retryable: Whether this failure is transient (retry) or permanent (dead-letter)
 * 
 * Why retryable flag:
 *   - SMTP timeout → retryable (server might recover)
 *   - Invalid email address → NOT retryable (permanent failure)
 *   - Distinguishing these prevents infinite retry loops
 */
@Getter
public class NotificationException extends RuntimeException {

    private final String errorCode;
    private final String channel;
    private final boolean retryable;
    private final HttpStatus status;

    public NotificationException(String message, String errorCode, String channel,
                                  boolean retryable, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.channel = channel;
        this.retryable = retryable;
        this.status = status;
    }

    public NotificationException(String message, String errorCode, String channel,
                                  boolean retryable, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.channel = channel;
        this.retryable = retryable;
        this.status = status;
    }

    // ──────────────── Factory Methods (Intent-Revealing) ────────────────

    public static NotificationException emailDeliveryFailed(String recipient, Throwable cause) {
        return new NotificationException(
                "Email delivery failed for: " + recipient,
                "NOTIF_EMAIL_001",
                "EMAIL",
                true,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }

    public static NotificationException invalidRecipient(String recipient) {
        return new NotificationException(
                "Invalid recipient address: " + recipient,
                "NOTIF_EMAIL_002",
                "EMAIL",
                false,  // Not retryable — permanent failure
                HttpStatus.BAD_REQUEST
        );
    }

    public static NotificationException smsDeliveryFailed(String phoneNumber, Throwable cause) {
        return new NotificationException(
                "SMS delivery failed for: " + phoneNumber,
                "NOTIF_SMS_001",
                "SMS",
                true,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }

    public static NotificationException pushDeliveryFailed(String userId, Throwable cause) {
        return new NotificationException(
                "Push notification failed for user: " + userId,
                "NOTIF_PUSH_001",
                "PUSH",
                true,
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }

    public static NotificationException templateNotFound(String templateName) {
        return new NotificationException(
                "Notification template not found: " + templateName,
                "NOTIF_TMPL_001",
                "ALL",
                false,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public static NotificationException rateLimitExceeded(String userId) {
        return new NotificationException(
                "Notification rate limit exceeded for user: " + userId,
                "NOTIF_RATE_001",
                "ALL",
                true,
                HttpStatus.TOO_MANY_REQUESTS
        );
    }
}
