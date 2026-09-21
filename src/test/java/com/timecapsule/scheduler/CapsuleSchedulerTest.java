package com.timecapsule.scheduler;

import com.timecapsule.core.Memory;
import com.timecapsule.core.Mood;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves once-only announcements, failure tolerance, and the poll loop. */
class CapsuleSchedulerTest {

    private Memory capsule(long id, String title) {
        Instant past = Instant.now().minusSeconds(60);
        return new Memory(id, 1, title, "opened", Mood.HAPPY, List.of(),
                true, past, past, past);
    }

    @Test
    void announcesEachSealExactlyOnce() {
        List<String> heard = new CopyOnWriteArrayList<>();
        CapsuleScheduler scheduler = new CapsuleScheduler(
                () -> List.of(capsule(1, "A"), capsule(2, "B")), Duration.ofSeconds(30));
        scheduler.addListener(memory -> heard.add(memory.getTitle()));

        assertEquals(2, scheduler.checkNow().size());
        assertEquals(0, scheduler.checkNow().size(), "Second check must stay silent");
        assertEquals(List.of("A", "B"), heard);
    }

    @Test
    void silenceWhenNothingIsDue() {
        AtomicInteger calls = new AtomicInteger();
        CapsuleScheduler scheduler = new CapsuleScheduler(List::of, Duration.ofSeconds(30));
        scheduler.addListener(memory -> calls.incrementAndGet());

        assertTrue(scheduler.checkNow().isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void sickSourceDoesNotBreakTheWatcher() {
        AtomicInteger calls = new AtomicInteger();
        CapsuleScheduler scheduler = new CapsuleScheduler(() -> {
            throw new RuntimeException("signed out");
        }, Duration.ofSeconds(30));
        scheduler.addListener(memory -> calls.incrementAndGet());

        assertTrue(scheduler.checkNow().isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void sickListenerDoesNotSilenceOthers() {
        List<String> heard = new CopyOnWriteArrayList<>();
        CapsuleScheduler scheduler = new CapsuleScheduler(
                () -> List.of(capsule(9, "Loud")), Duration.ofSeconds(30));
        scheduler.addListener(memory -> {
            throw new RuntimeException("broken horn");
        });
        scheduler.addListener(memory -> heard.add(memory.getTitle()));

        scheduler.checkNow();
        assertEquals(List.of("Loud"), heard);
    }

    @Test
    void backgroundLoopPollsAndStops() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CapsuleScheduler scheduler = new CapsuleScheduler(
                () -> List.of(capsule(5, "Tick")), Duration.ofMillis(100));
        scheduler.addListener(memory -> latch.countDown());

        scheduler.start();
        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS), "Daemon poll should fire");
            assertTrue(scheduler.isRunning());
        } finally {
            scheduler.stop();
        }
        assertFalse(scheduler.isRunning());
    }

    @Test
    void constructorGuardsItsInputs() {
        try {
            new CapsuleScheduler(null, Duration.ofSeconds(1));
            assertTrue(false, "null source must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
        try {
            new CapsuleScheduler(List::of, Duration.ZERO);
            assertTrue(false, "non-positive period must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }
}
