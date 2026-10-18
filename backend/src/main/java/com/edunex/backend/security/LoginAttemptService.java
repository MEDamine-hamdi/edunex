package com.edunex.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory brute-force protection: too many failures for a key block it for a while. */
@Component
public class LoginAttemptService {

    private record Attempt(int failures, Instant lastFailure) {
    }

    private final int maxFailures;
    private final Duration lockDuration;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.security.login-max-failures:5}") int maxFailures,
                               @Value("${app.security.login-lock-minutes:15}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isBlocked(String key) {
        return isBlocked(key, Instant.now());
    }

    boolean isBlocked(String key, Instant now) {
        Attempt a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (!now.isBefore(a.lastFailure().plus(lockDuration))) {
            attempts.remove(key);
            return false;
        }
        return a.failures() >= maxFailures;
    }

    /** Returns true when this failure triggers (or keeps) the lock. */
    public boolean recordFailure(String key) {
        return recordFailure(key, Instant.now());
    }

    boolean recordFailure(String key, Instant now) {
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> !now.isBefore(e.getValue().lastFailure().plus(lockDuration)));
        }
        Attempt updated = attempts.merge(key, new Attempt(1, now), (old, ignored) ->
                now.isBefore(old.lastFailure().plus(lockDuration))
                        ? new Attempt(old.failures() + 1, now)
                        : new Attempt(1, now));
        return updated.failures() >= maxFailures;
    }

    public long retryAfterSeconds(String key) {
        return retryAfterSeconds(key, Instant.now());
    }

    long retryAfterSeconds(String key, Instant now) {
        Attempt a = attempts.get(key);
        if (a == null) {
            return 0;
        }
        return Math.max(0, Duration.between(now, a.lastFailure().plus(lockDuration)).getSeconds());
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}