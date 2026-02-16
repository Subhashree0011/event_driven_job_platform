package com.platform.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Global exception handler for Notification Service.
 * 
 * Converts exceptions to structured JSON error responses.
 * Logs at appropriate severity based on retryability.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<Map<String, Object>> handleNotificationException(NotificationException ex) {
        if (ex.isRetryable()) {
            log.warn("Retryable notification failure: {} [channel={}, code={}]",
                    ex.getMessage(), ex.getChannel(), ex.getErrorCode());
        } else {
            log.error("Non-retryable notification failure: {} [channel={}, code={}]",
                    ex.getMessage(), ex.getChannel(), ex.getErrorCode(), ex);
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", ex.getStatus().value(),
                "error", ex.getStatus().getReasonPhrase(),
                "code", Objects.toString(ex.getErrorCode(), "UNKNOWN"),
                "channel", Objects.toString(ex.getChannel(), "UNKNOWN"),
                "retryable", ex.isRetryable(),
                "message", Objects.toString(ex.getMessage(), "Notification delivery failed")
        );

        return ResponseEntity.status(ex.getStatus().value()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error in notification-service", ex);

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
