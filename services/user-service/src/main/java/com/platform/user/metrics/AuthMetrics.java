package com.platform.user.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Authentication metrics for Prometheus monitoring.
 * 
 * Metrics tracked:
 * - auth.login.attempts — total login attempts
 * - auth.login.success — successful logins
 * - auth.login.failure — failed logins
 * - auth.registration.attempts — total registration attempts
 * - auth.registration.success — successful registrations
 */
@Component
public class AuthMetrics {

    private final Counter loginAttempts;
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter registrationAttempts;
    private final Counter registrationSuccess;

    public AuthMetrics(MeterRegistry registry) {
        this.loginAttempts = Counter.builder("auth.login.attempts")
                .description("Total login attempts")
                .register(registry);
        this.loginSuccess = Counter.builder("auth.login.success")
                .description("Successful logins")
                .register(registry);
        this.loginFailure = Counter.builder("auth.login.failure")
                .description("Failed logins")
                .register(registry);
        this.registrationAttempts = Counter.builder("auth.registration.attempts")
                .description("Total registration attempts")
                .register(registry);
        this.registrationSuccess = Counter.builder("auth.registration.success")
                .description("Successful registrations")
                .register(registry);
    }

    public void incrementLoginAttempt() { loginAttempts.increment(); }
    public void incrementLoginSuccess() { loginSuccess.increment(); }
    public void incrementLoginFailure() { loginFailure.increment(); }
    public void incrementRegistrationAttempt() { registrationAttempts.increment(); }
    public void incrementRegistrationSuccess() { registrationSuccess.increment(); }
}
