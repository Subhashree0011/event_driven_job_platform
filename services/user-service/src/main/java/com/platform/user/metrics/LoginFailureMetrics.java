package com.platform.user.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Tracks login failure patterns for security monitoring.
 * Alerts can be configured in Grafana for anomaly detection.
 */
@Component
public class LoginFailureMetrics {

    private final Counter invalidCredentials;
    private final Counter accountLocked;
    private final Counter accountSuspended;

    public LoginFailureMetrics(MeterRegistry registry) {
        this.invalidCredentials = Counter.builder("auth.failure.invalid_credentials")
                .description("Failed login: invalid credentials")
                .register(registry);
        this.accountLocked = Counter.builder("auth.failure.account_locked")
                .description("Failed login: account locked")
                .register(registry);
        this.accountSuspended = Counter.builder("auth.failure.account_suspended")
                .description("Failed login: account suspended")
                .register(registry);
    }

    public void recordInvalidCredentials() { invalidCredentials.increment(); }
    public void recordAccountLocked() { accountLocked.increment(); }
    public void recordAccountSuspended() { accountSuspended.increment(); }
}
