package com.micatechnologies.minecraft.ldprofilerguard.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The decisions behind the {@code Profiler} mixin, kept free of Minecraft classes so they are
 * unit-tested directly.
 *
 * <p>Vanilla's profiler keeps open section names and start times in two lists, and
 * {@code endSection} pops both with {@code remove(size() - 1)}. Two things make that throw
 * {@code ArrayIndexOutOfBoundsException: -1} and take the game down while profiling is on:
 * <ul>
 *   <li>a caller that skips its {@code startSection} (it checked {@code profilingEnabled} while it
 *       was off) but still calls {@code endSection} after profiling was switched on;</li>
 *   <li>a thread other than the one that owns the profiler (an async worker, a FAWE task) opening
 *       or closing sections on it, which unbalances the owner's stack.</li>
 * </ul>
 *
 * <p>One guard belongs to one profiler. The owner is the thread that opens the {@code "root"}
 * section, which vanilla does at the top of every server tick and every client frame. Until it
 * has been seen, every thread is allowed, as in vanilla.
 */
public final class ProfilerGuard {

    /** Name of the section the owning thread opens first each tick or frame. */
    public static final String ROOT = "root";

    private final GuardLog log;
    private volatile Thread owner;
    private final AtomicLong underflows = new AtomicLong();
    private final AtomicLong foreignCalls = new AtomicLong();

    public ProfilerGuard(GuardLog log) {
        this.log = log;
    }

    /**
     * Whether a {@code startSection(name)} call from {@code caller} should be ignored. Opening
     * {@code "root"} makes the caller the owner.
     */
    public boolean skipStart(String name, Thread caller) {
        if (ROOT.equals(name)) {
            owner = caller;
            return false;
        }
        return isForeign(caller, "startSection(" + name + ")");
    }

    /** Whether an {@code endStartSection(name)} call from {@code caller} should be ignored. */
    public boolean skipEndStart(String name, Thread caller) {
        return isForeign(caller, "endStartSection(" + name + ")");
    }

    /**
     * Whether an {@code endSection} call should be ignored: from a foreign thread, or with
     * nothing open to pop.
     */
    public boolean skipEnd(int openSections, int openTimestamps, Thread caller) {
        if (isForeign(caller, "endSection")) {
            return true;
        }
        if (openSections <= 0 || openTimestamps <= 0) {
            long n = underflows.incrementAndGet();
            log.event(GuardLog.Kind.UNDERFLOW, n, "endSection with no open section on thread '"
                    + caller.getName() + "' (sections=" + openSections + ", timestamps=" + openTimestamps + ")");
            return true;
        }
        return false;
    }

    private boolean isForeign(Thread caller, String call) {
        Thread o = owner;
        if (o == null || o == caller) {
            return false;
        }
        long n = foreignCalls.incrementAndGet();
        log.event(GuardLog.Kind.FOREIGN_THREAD, n, call + " from thread '" + caller.getName()
                + "', profiler is owned by '" + o.getName() + "'");
        return true;
    }

    public long underflows() {
        return underflows.get();
    }

    public long foreignCalls() {
        return foreignCalls.get();
    }

    public Thread owner() {
        return owner;
    }
}
