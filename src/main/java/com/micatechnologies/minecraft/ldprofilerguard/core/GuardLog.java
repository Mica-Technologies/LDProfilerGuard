package com.micatechnologies.minecraft.ldprofilerguard.core;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Rate-limited reporting for {@link ProfilerGuard}. The first event of each kind is written with
 * a stack trace, so the offending caller can be found. After that, at most one line per kind per
 * interval, carrying the running count, so a caller that misbehaves every tick cannot flood the
 * log.
 */
public final class GuardLog {

    public enum Kind { UNDERFLOW, FOREIGN_THREAD }

    /** Where lines go; {@code trace} is null for the rate-limited follow-up lines. */
    public interface Sink {
        void write(Kind kind, String message, Throwable trace);
    }

    public static final long DEFAULT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);

    private final Sink sink;
    private final LongSupplier clock;
    private final long intervalNanos;
    private final Map<Kind, Long> lastLogged = new EnumMap<>(Kind.class);

    public GuardLog(Sink sink, LongSupplier clock, long intervalNanos) {
        this.sink = sink;
        this.clock = clock;
        this.intervalNanos = intervalNanos;
    }

    /** Records the {@code count}-th event of {@code kind}; writes it if the rate limit allows. */
    public void event(Kind kind, long count, String detail) {
        long now = clock.getAsLong();
        boolean first;
        synchronized (lastLogged) {
            Long last = lastLogged.get(kind);
            first = last == null;
            if (!first && now - last < intervalNanos) {
                return;
            }
            lastLogged.put(kind, now);
        }
        if (first) {
            sink.write(kind, "Profiler guard ignored " + detail
                    + " instead of crashing. Stack trace of the first one follows;"
                    + " later ones are summarised at most once per interval.",
                    new Throwable("first " + kind + " call site"));
        } else {
            sink.write(kind, "Profiler guard: " + count + " " + kind + " call(s) ignored so far; latest: " + detail,
                    null);
        }
    }
}
