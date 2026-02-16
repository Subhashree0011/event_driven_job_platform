package com.platform.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.platform.notification.exception.NotificationException;
import com.platform.notification.metrics.NotificationLatencyMetrics;

/**
 * Push notification delivery service.
 * 
 * Currently a structured placeholder — in production this would integrate with
 * Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNs).
 * 
 * Design decisions:
 *   - Uses the general asyncExecutor pool (push is lightweight)
 *   - No circuit breaker needed (FCM has built-in retry)
 *   - Device token management would be a separate concern
 * 
 * Why push is separate from email/SMS:
 *   - Different delivery semantics (fire-and-forget vs delivery receipt)
 *   - Different payload format (JSON vs HTML vs plain text)
 *   - Device token rotation and invalidation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final NotificationLatencyMetrics latencyMetrics;

    /**
     * Send a push notification to a user's device.
     * 
     * @param userId Target user ID
     * @param title Notification title
     * @param body Notification body text
     * @throws NotificationException if delivery fails
     */
    @Async("asyncExecutor")
    public void sendPush(String userId, String title, String body) {
        long start = System.currentTimeMillis();

        try {
            // In production: integrate with FCM/APNs
            // Message message = Message.builder()
            //     .setToken(deviceToken)
            //     .setNotification(Notification.builder()
            //         .setTitle(title).setBody(body).build())
            //     .build();
            // FirebaseMessaging.getInstance().send(message);

            log.info("Push notification sent: userId={}, title={}", userId, title);

            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("push", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            latencyMetrics.recordSendLatency("push", duration);
            log.error("Push notification failed: userId={}", userId, e);
            throw NotificationException.pushDeliveryFailed(userId, e);
        }
    }
}
