package com.platform.application.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder consumer for email notification events.
 * In the full architecture, this logic lives in the notification-service.
 * This consumer exists to handle any email-related events routed to the application-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    // Email notifications are handled by notification-service, not application-service.
    // This class exists as a placeholder in case application-service needs to
    // consume email delivery status events in the future.
}
