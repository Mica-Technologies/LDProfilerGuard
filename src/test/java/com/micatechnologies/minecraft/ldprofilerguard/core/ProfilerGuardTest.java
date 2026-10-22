package com.micatechnologies.minecraft.ldprofilerguard.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ProfilerGuardTest {

    private static final class Line {
        final GuardLog.Kind kind;
        final String message;
        final Throwable trace;

        Line(GuardLog.Kind kind, String message, Throwable trace) {
            this.kind = kind;
            this.message = message;
            this.trace = trace;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final AtomicLong now = new AtomicLong();
    private final GuardLog log = new GuardLog((k, m, t) -> lines.add(new Line(k, m, t)), now::get,
            TimeUnit.SECONDS.toNanos(60));

    @Test
    void balancedCallsPassThrough() {
        ProfilerGuard g = new ProfilerGuard(log);
        Thread me = Thread.currentThread();
        assertFalse(g.skipStart("root", me));
        assertFalse(g.skipStart("tick", me));
        assertFalse(g.skipEnd(2, 2, me));
        assertFalse(g.skipEndStart("next", me));
        assertEquals(0, g.underflows());
        assertEquals(0, g.foreignCalls());
        assertTrue(lines.isEmpty());
    }

    @Test
    void endSectionOnEmptyStackIsSkippedAndCounted() {
        ProfilerGuard g = new ProfilerGuard(log);
        Thread me = Thread.currentThread();
        assertTrue(g.skipEnd(0, 0, me));
        assertTrue(g.skipEnd(0, 1, me));
        assertTrue(g.skipEnd(1, 0, me));
        assertEquals(3, g.underflows());
    }

    @Test
    void underflowBeforeOwnerKnownStillGuarded() {
        ProfilerGuard g = new ProfilerGuard(log);
        assertNull(g.owner());
        assertTrue(g.skipEnd(0, 0, Thread.currentThread()));
    }

    @Test
    void rootSectionClaimsOwnership() {
        ProfilerGuard g = new ProfilerGuard(log);
        Thread me = Thread.currentThread();
        g.skipStart("root", me);
        assertSame(me, g.owner());
    }

    @Test
    void anyThreadAllowedUntilOwnerKnown() throws Exception {
        ProfilerGuard g = new ProfilerGuard(log);
        AtomicBoolean skipped = new AtomicBoolean(true);
        Thread t = new Thread(() -> skipped.set(g.skipStart("x", Thread.currentThread())));
        t.start();
        t.join();
        assertFalse(skipped.get());
    }

    @Test
    void foreignThreadCallsAreIgnored() throws Exception {
        ProfilerGuard g = new ProfilerGuard(log);
        g.skipStart("root", Thread.currentThread());
        boolean[] skipped = new boolean[3];
        Thread t = new Thread(() -> {
            Thread self = Thread.currentThread();
            skipped[0] = g.skipStart("async", self);
            skipped[1] = g.skipEnd(5, 5, self);
            skipped[2] = g.skipEndStart("async2", self);
        }, "worker");
        t.start();
        t.join();
        assertTrue(skipped[0] && skipped[1] && skipped[2]);
        assertEquals(3, g.foreignCalls());
        assertEquals(0, g.underflows(), "a foreign endSection is not also an underflow");
    }

    @Test
    void firstEventLoggedWithTraceThenRateLimited() {
        ProfilerGuard g = new ProfilerGuard(log);
        Thread me = Thread.currentThread();
        g.skipEnd(0, 0, me);
        assertEquals(1, lines.size());
        assertNotNull(lines.get(0).trace);
        assertEquals(GuardLog.Kind.UNDERFLOW, lines.get(0).kind);

        now.addAndGet(TimeUnit.SECONDS.toNanos(10));
        for (int i = 0; i < 100; i++) {
            g.skipEnd(0, 0, me);
        }
        assertEquals(1, lines.size(), "suppressed inside the interval");

        now.addAndGet(TimeUnit.SECONDS.toNanos(60));
        g.skipEnd(0, 0, me);
        assertEquals(2, lines.size());
        assertNull(lines.get(1).trace);
        assertTrue(lines.get(1).message.contains("102"), lines.get(1).message);
    }

    @Test
    void kindsAreRateLimitedSeparately() throws Exception {
        ProfilerGuard g = new ProfilerGuard(log);
        g.skipStart("root", Thread.currentThread());
        g.skipEnd(0, 0, Thread.currentThread());
        Thread t = new Thread(() -> g.skipStart("a", Thread.currentThread()));
        t.start();
        t.join();
        assertEquals(2, lines.size());
        assertEquals(GuardLog.Kind.FOREIGN_THREAD, lines.get(1).kind);
        assertNotNull(lines.get(1).trace);
    }
}
