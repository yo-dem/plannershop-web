package it.plannershop.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import it.plannershop.common.ApiException;

/**
 * Limits failed sign-in attempts per IP address and per email, to slow down
 * password guessing.
 *
 * <p>In memory: reset at restart and valid for a single backend instance,
 * enough for the intended use.
 */
@Component
public class LoginAttemptLimiter {

    private final SecurityProperties props;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(SecurityProperties props) {
        this.props = props;
    }

    public void check(String ip, String email) {
        if (blocked(ipKey(ip)) || blocked(emailKey(email))) {
            throw ApiException.tooManyRequests("auth.tooManyAttempts");
        }
    }

    public void recordFailure(String ip, String email) {
        add(ipKey(ip));
        add(emailKey(email));
    }

    public void recordSuccess(String email) {
        failures.remove(emailKey(email));
    }

    private boolean blocked(String key) {
        Deque<Instant> list = failures.get(key);
        if (list == null) {
            return false;
        }
        synchronized (list) {
            prune(list);
            return list.size() >= props.loginMaxAttempts();
        }
    }

    private void add(String key) {
        Deque<Instant> list = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (list) {
            prune(list);
            list.addLast(clock.instant());
        }
    }

    private void prune(Deque<Instant> list) {
        Instant limit = clock.instant().minus(props.loginWindow());
        while (!list.isEmpty() && list.peekFirst().isBefore(limit)) {
            list.removeFirst();
        }
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }

    private static String emailKey(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}
