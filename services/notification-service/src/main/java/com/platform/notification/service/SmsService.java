package com.platform.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.platform.notification.exception.NotificationException;
import com.platform.notification.metrics.NotificationLatencyMetrics;

/**
 * SMS delivery service.
 * 
 * Currently a structured placeholder — in production this would integrate with
 * a provider like Twilio, AWS SNS, or MessageBird.
 * 
 * Design decisions:
 *   - Separate circuit breaker instance (smsCircuitBreaker)
 *   - Dedicated thread pool (smsExecutor) — bulkhead isolation from email
 *   - Rate limiting at provider level (SMS APIs are typically rate-limited)
 * 
 * Why separate from EmailService:
 *   - Different failure modes (SMTP vs REST API)
 *   - Different rate limits
 *   - Different cost implications (SMS costs money per message)
 *   - Independent circuit breakers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final NotificationLatencyMetrics latencyMetrics;

    /**
     * Send an SMS notification.
     * 
     * @param phoneNumber Recipient phone number (E.164 format)
     * @param message SMS text content (max 160 chars for single segment)
     * @throws NotificationException if delivery fails
     */
    @CircuitBreaker(name = "smsCircuitBreaker", fallbackMethod = "smsFallback")
    @Async("smsExecutor")
    public void sendSms(String phoneNumber, String message) {
        long start = System.currentTimeMillis();

        try {
            // In production: integrate with Twilio/SNS/MessageBird
            // twilioClient.messages()
            //     .create(new PhoneNumber(phoneNumber), new PhoneNumber(fromNumber), message);

            // Simulate SMS sending for now
            log.info("SMS sent: to={}, length={} chars", phoneNumber, message.length());

            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("sms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("sms", duration);
            log.error("SMS delivery failed: to={}", phoneNumber, e);
            throw NotificationException.smsDeliveryFailed(phoneNumber, e);
        }
    }

    /**
     * Circuit breaker fallback for SMS.
     */
    @SuppressWarnings("unused")
    private void smsFallback(String phoneNumber, String message, Throwable t) {
        log.warn("SMS circuit breaker OPEN — skipping SMS to {}. Reason: {}",
                phoneNumber, t.getMessage());
        latencyMetrics.recordCircuitBreakerOpen("sms");
    }
}
