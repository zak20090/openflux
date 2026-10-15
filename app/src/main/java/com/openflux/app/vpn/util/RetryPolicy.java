package com.openflux.app.vpn.util;

/** Экспоненциальный backoff: base * 2^attempt, ограниченный maxMs, с небольшим jitter. */
public class RetryPolicy {
    private final long baseMs;
    private final long maxMs;

    public RetryPolicy(long baseMs, long maxMs) {
        if (baseMs <= 0 || maxMs < baseMs) throw new IllegalArgumentException("bad backoff");
        this.baseMs = baseMs;
        this.maxMs = maxMs;
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(1000, 30000);
    }

    /** @param attempt номер попытки с 0 */
    public long nextDelayMs(int attempt) {
        int shift = Math.min(Math.max(attempt, 0), 20);
        long delay = Math.min(maxMs, baseMs << shift);
        long jitter = (long) (delay * 0.1 * Math.random());
        return Math.min(maxMs, delay + jitter);
    }
}
