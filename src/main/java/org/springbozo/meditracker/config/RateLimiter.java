package org.springbozo.meditracker.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory fixed-window counters. Fine for a single backend instance;
 * counts reset when the app restarts.
 */
@Component
public class RateLimiter {

    private record Window(long start, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    /** Counts one hit for the key and returns false if that goes over the limit. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        return current(key, window).count().incrementAndGet() <= limit;
    }

    /** True if the key has already used up its limit, without counting a hit. */
    public boolean isOverLimit(String key, int limit, Duration window) {
        Window w = windows.get(key);
        return w != null && !expired(w, window) && w.count().get() >= limit;
    }

    public void hit(String key, Duration window) {
        current(key, window).count().incrementAndGet();
    }

    public void clear(String key) {
        windows.remove(key);
    }

    public void reset() {
        windows.clear();
    }

    private Window current(String key, Duration window) {
        // drop old windows now and then so the map can't grow forever
        if (calls.incrementAndGet() % 1000 == 0) {
            long cutoff = System.currentTimeMillis() - Duration.ofDays(1).toMillis();
            windows.values().removeIf(w -> w.start() < cutoff);
        }
        return windows.compute(key, (k, w) ->
                w == null || expired(w, window) ? new Window(System.currentTimeMillis(), new AtomicInteger()) : w);
    }

    private static boolean expired(Window w, Duration window) {
        return System.currentTimeMillis() - w.start() >= window.toMillis();
    }
}
