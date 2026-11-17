package com.ecoswap.auth.service;

import com.ecoswap.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * In-memory throttling for login and password-reset requests (per e-mail key).
 * After N failures inside the window the key is locked for a few minutes.
 * For a multi-instance deployment this would move to a shared store.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(AppProperties props) {
        this.maxAttempts = props.auth().maxLoginAttempts();
        this.lockDuration = Duration.ofMinutes(props.auth().loginLockMinutes());
    }

    public boolean isBlocked(String key) {
        Attempts a = attempts.get(key);
        return a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(Instant.now());
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, a) -> {
            if (a == null || a.windowStart().plus(lockDuration).isBefore(now)) {
                return new Attempts(1, now, null);
            }
            int failures = a.failures() + 1;
            return new Attempts(failures, a.windowStart(), failures >= maxAttempts ? now.plus(lockDuration) : a.lockedUntil());
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    public long minutesLocked() {
        return lockDuration.toMinutes();
    }

    @Scheduled(fixedDelay = 600_000)
    void purgeExpired() {
        Instant cutoff = Instant.now().minus(lockDuration.multipliedBy(2));
        attempts.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff)
                && (e.getValue().lockedUntil() == null || e.getValue().lockedUntil().isBefore(Instant.now())));
    }
}
