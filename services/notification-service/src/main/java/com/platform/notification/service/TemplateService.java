package com.platform.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.platform.notification.exception.NotificationException;

import java.util.Map;
import java.util.Objects;

/**
 * Thymeleaf-based template rendering for notification content.
 * 
 * Templates stored in: classpath:/templates/
 * Supported templates:
 *   - application-confirmation.html — sent when user applies to a job
 *   - job-match.html — sent when a new job matches user criteria
 *   - status-update.html — sent when application status changes
 * 
 * Why Thymeleaf:
 *   - Spring Boot native integration
 *   - HTML email templates with variables
 *   - Testable independently from email sending
 *   - Template caching in production
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Render a notification template with the given variables.
     * 
     * @param templateName Template file name without extension (e.g., "application-confirmation")
     * @param variables Key-value pairs to inject into the template
     * @return Rendered HTML string
     * @throws NotificationException if template cannot be found or rendered
     */
    public String render(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }

            String rendered = templateEngine.process(templateName, context);
            log.debug("Template rendered successfully: {}", templateName);
            return rendered;

        } catch (Exception e) {
            log.error("Failed to render template: {}", templateName, e);
            throw NotificationException.templateNotFound(templateName);
        }
    }

    /**
     * Render the application confirmation email template.
     */
    public String renderApplicationConfirmation(String userName, String jobTitle,
                                                  String companyName, Long applicationId) {
        Map<String, Object> vars = Map.of(
                "userName", Objects.toString(userName, "User"),
                "jobTitle", Objects.toString(jobTitle, "Job"),
                "companyName", Objects.toString(companyName, "Company"),
                "applicationId", applicationId != null ? applicationId : 0L,
                "year", java.time.Year.now().getValue()
        );
        return render("application-confirmation", vars);
    }

    /**
     * Render the application status update email template.
     */
    public String renderStatusUpdate(String userName, String jobTitle,
                                      String oldStatus, String newStatus) {
        Map<String, Object> vars = Map.of(
                "userName", Objects.toString(userName, "User"),
                "jobTitle", Objects.toString(jobTitle, "Job"),
                "oldStatus", Objects.toString(oldStatus, "Unknown"),
                "newStatus", Objects.toString(newStatus, "Unknown"),
                "year", java.time.Year.now().getValue()
        );
        return render("status-update", vars);
    }

    /**
     * Render a plain text fallback when HTML template fails.
     */
    public String renderPlainTextFallback(String eventType, Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("Notification: ").append(eventType).append("\n\n");
        if (metadata != null) {
            metadata.forEach((key, value) ->
                    sb.append(key).append(": ").append(value).append("\n"));
        }
        return sb.toString();
    }
}
