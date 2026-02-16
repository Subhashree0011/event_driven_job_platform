package com.platform.user.resilience;

import org.springframework.stereotype.Component;

/**
 * Bulkhead pattern for login operations.
 * 
 * Configured via application.yml (resilience4j.bulkhead.instances.loginBulkhead):
 * - max-concurrent-calls: 20
 * - max-wait-duration: 500ms
 * 
 * This prevents login storms from consuming all threads and
 * starving other operations (profile updates, token refresh).
 * 
 * Applied via @Bulkhead annotation on service methods.
 */
@Component
public class LoginBulkhead {
    // Configuration is declarative via @Bulkhead annotation + application.yml
    // This class provides a clear place for any programmatic bulkhead customization
}
