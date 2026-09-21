package com.timecapsule.scheduler;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.core.Memory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Background seal-checker (Module: Capsule Scheduler). Polls a due-source
 * (production: {@code JournalManager::listDueEntries}) on a daemon thread
 * and notifies {@link CapsuleListener}s once per entry. Never throws out of
 * a poll — a signed-out keeper or a sick listener just means "try later".
 */
public class CapsuleScheduler {

    private final Supplier<List<Memory>> dueSource;
    private final Duration period;
    private final List<CapsuleListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<Long> announced = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService executor;

    public CapsuleScheduler(Supplier<List<Memory>> dueSource, Duration period) {
        if (dueSource == null || period == null || period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("Scheduler needs a source and a positive period.");
        }
        this.dueSource = dueSource;
        this.period = period;
    }

    /** Starts the daemon poll loop (idempotent). */
    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "capsule-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::poll,
                period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Stops the loop; announced seals are forgotten (fresh on next start). */
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        announced.clear();
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    public void addListener(CapsuleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CapsuleListener listener) {
        listeners.remove(listener);
    }

    /**
     * Checks seals right now (login refresh, tests) and returns the newly
     * opened entries. Runs synchronously on the caller's thread.
     */
    public List<Memory> checkNow() {
        return poll();
    }

    private List<Memory> poll() {
        List<Memory> due;
        try {
            due = dueSource.get();
        } catch (Exception e) {
            return List.of(); // Signed out, locked DB, bad day — retry next poll.
        }
        if (due == null || due.isEmpty()) {
            return List.of();
        }
        List<Memory> opened = new ArrayList<>();
        for (Memory memory : due) {
            if (memory == null || !announced.add(memory.getId())) {
                continue;
            }
            opened.add(memory);
            AuditLogger.log(null, "capsule.opened " + memory.getId());
            for (CapsuleListener listener : listeners) {
                try {
                    listener.onCapsuleOpened(memory);
                } catch (Exception ignored) {
                    // One sick listener must not silence the rest.
                }
            }
        }
        return opened;
    }
}
