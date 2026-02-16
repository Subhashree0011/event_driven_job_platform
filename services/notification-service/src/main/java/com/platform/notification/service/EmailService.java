package com.platform.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.platform.notification.exception.NotificationException;
import com.platform.notification.metrics.NotificationLatencyMetrics;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * Email delivery service using Spring Boot JavaMailSender (SMTP).
 * 
 * Features:
 *   - Circuit breaker on SMTP calls (emailCircuitBreaker)
 *   - Async execution on dedicated email thread pool
 *   - HTML content via Thymeleaf templates
 *   - Latency tracking via Micrometer
 * 
 * Why circuit breaker on email:
 *   - SMTP servers can be slow/unavailable
 *   - Without CB, all email threads block → thread pool exhaustion
 *   - CB opens after 50% failure rate → fail fast, route to retry topic
 * 
 * Why async:
 *   - Kafka consumer thread should not block on SMTP I/O
 *   - Email sending can take 1-5 seconds per message
 *   - Dedicated pool prevents starvation of other consumers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final NotificationLatencyMetrics latencyMetrics;

    private static final String FROM_ADDRESS = "noreply@jobplatform.com";
    private static final String FROM_NAME = "Job Platform";

    /**
     * Send an HTML email notification.
     * 
     * @param to Recipient email address
     * @param subject Email subject line
     * @param templateName Thymeleaf template name
     * @param variables Template variables
     * @throws NotificationException if delivery fails
     */
    @CircuitBreaker(name = "emailCircuitBreaker", fallbackMethod = "emailFallback")
    @Async("emailExecutor")
    @SuppressWarnings("null")
    public void sendEmail(String to, String subject, String templateName,
                           Map<String, Object> variables) {
        long start = System.currentTimeMillis();

        try {
            String htmlContent = templateService.render(templateName, variables);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("email", duration);
            log.info("Email sent successfully: to={}, subject={}, duration={}ms",
                    to, subject, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("email", duration);
            log.error("Email delivery failed: to={}, subject={}", to, subject, e);
            throw NotificationException.emailDeliveryFailed(to, e);
        }
    }

    /**
     * Send a simple text email (no template).
     */
    @CircuitBreaker(name = "emailCircuitBreaker", fallbackMethod = "emailFallback")
    @Async("emailExecutor")
    @SuppressWarnings("null")
    public void sendSimpleEmail(String to, String subject, String text) {
        long start = System.currentTimeMillis();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(message);

            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("email", duration);
            log.info("Simple email sent: to={}, duration={}ms", to, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("email", duration);
            throw NotificationException.emailDeliveryFailed(to, e);
        }
    }

    /**
     * Circuit breaker fallback — logs failure, does NOT retry here.
     * Retry is handled by RetryHandler via Kafka retry topic.
     */
    @SuppressWarnings("unused")
    private void emailFallback(String to, String subject, String templateName,
                                Map<String, Object> variables, Throwable t) {
        log.warn("Email circuit breaker OPEN — skipping delivery to {}. Reason: {}",
                to, t.getMessage());
        latencyMetrics.recordCircuitBreakerOpen("email");
    }

    @SuppressWarnings("unused")
    private void emailFallback(String to, String subject, String text, Throwable t) {
        log.warn("Email circuit breaker OPEN — skipping simple email to {}. Reason: {}",
                to, t.getMessage());
        latencyMetrics.recordCircuitBreakerOpen("email");
    }
}
