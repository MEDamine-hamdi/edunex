package com.edunex.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private static final String KEY = "user@test.tn|127.0.0.1";

    private final LoginAttemptService service = new LoginAttemptService(3, 15);
    private final Instant t0 = Instant.parse("2026-10-18T10:00:00Z");

    @Test
    void blocksAfterTheConfiguredNumberOfFailures() {
        assertThat(service.recordFailure(KEY, t0)).isFalse();
        assertThat(service.recordFailure(KEY, t0.plusSeconds(1))).isFalse();
        assertThat(service.isBlocked(KEY, t0.plusSeconds(1))).isFalse();

        assertThat(service.recordFailure(KEY, t0.plusSeconds(2))).isTrue();
        assertThat(service.isBlocked(KEY, t0.plusSeconds(3))).isTrue();
    }

    @Test
    void otherKeysAreNotAffected() {
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0);

        assertThat(service.isBlocked("other@test.tn|127.0.0.1", t0.plusSeconds(1))).isFalse();
        assertThat(service.isBlocked("user@test.tn|10.0.0.9", t0.plusSeconds(1))).isFalse();
    }

    @Test
    void theBlockExpiresAfterTheLockDuration() {
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0.plusSeconds(1));
        service.recordFailure(KEY, t0.plusSeconds(2));

        assertThat(service.isBlocked(KEY, t0.plusSeconds(901))).isTrue();
        assertThat(service.isBlocked(KEY, t0.plusSeconds(902))).isFalse();
    }

    @Test
    void aSuccessfulLoginClearsTheFailures() {
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0.plusSeconds(1));
        service.reset(KEY);

        assertThat(service.recordFailure(KEY, t0.plusSeconds(2))).isFalse();
        assertThat(service.isBlocked(KEY, t0.plusSeconds(3))).isFalse();
    }

    @Test
    void retryAfterCountsDown() {
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0);
        service.recordFailure(KEY, t0);

        assertThat(service.retryAfterSeconds(KEY, t0.plusSeconds(60))).isEqualTo(840);
        assertThat(service.retryAfterSeconds("unknown", t0)).isZero();
    }
}