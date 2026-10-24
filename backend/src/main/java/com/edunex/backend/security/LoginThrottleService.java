package com.edunex.backend.security;

import com.edunex.backend.entity.LoginThrottle;
import com.edunex.backend.repository.LoginThrottleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoginThrottleService {

    private final LoginThrottleRepository repository;

    /** Seconds left on an active lock, 0 when the key is not locked. */
    @Transactional(readOnly = true)
    public long lockedSeconds(String key) {
        return repository.findById(key)
                .map(LoginThrottle::getLockedUntil)
                .map(until -> Duration.between(Instant.now(), until).toSeconds() + 1)
                .filter(s -> s > 0)
                .orElse(0L);
    }

    /** Records a failed login. Returns true when the key is (now) locked. */
    @Transactional
    public boolean recordFailure(String key, int max, int windowMinutes, int lockMinutes) {
        Instant now = Instant.now();
        LoginThrottle t = row(key);
        if (t.getLockedUntil() != null && t.getLockedUntil().isAfter(now)) {
            return true;
        }
        if (t.getLockedUntil() != null || expired(t, now, windowMinutes)) {
            t.setFailures(0);
            t.setWindowStart(now);
            t.setLockedUntil(null);
        }
        t.setFailures(t.getFailures() + 1);
        if (t.getFailures() >= max) {
            t.setLockedUntil(now.plus(lockMinutes, ChronoUnit.MINUTES));
            return true;
        }
        return false;
    }

    /** Counts one use of a limited action. Returns 0 when allowed, otherwise the seconds to wait. */
    @Transactional
    public long consume(String key, int max, int windowMinutes) {
        Instant now = Instant.now();
        LoginThrottle t = row(key);
        if (expired(t, now, windowMinutes)) {
            t.setFailures(0);
            t.setWindowStart(now);
        }
        if (t.getFailures() >= max) {
            return Math.max(1, Duration.between(now, t.getWindowStart().plus(windowMinutes, ChronoUnit.MINUTES)).toSeconds() + 1);
        }
        t.setFailures(t.getFailures() + 1);
        return 0;
    }

    @Transactional
    public void reset(String key) {
        repository.deleteById(key);
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT6H")
    @Transactional
    public void purge() {
        repository.purge(Instant.now().minus(2, ChronoUnit.DAYS), Instant.now());
    }

    private LoginThrottle row(String key) {
        repository.ensure(key);
        return repository.findForUpdate(key).orElseThrow();
    }

    private static boolean expired(LoginThrottle t, Instant now, int minutes) {
        return t.getWindowStart() == null || t.getWindowStart().plus(minutes, ChronoUnit.MINUTES).isBefore(now);
    }
}