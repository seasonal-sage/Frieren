package com.timecapsule.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Proves the page tracker remembers, ignores blanks, and resets. */
class PageTrackerTest {

    @Test
    void tracksAndResets() {
        PageTracker.reset();
        assertEquals("cover", PageTracker.current());
        PageTracker.set("entries");
        assertEquals("entries", PageTracker.current());
        PageTracker.set(null);
        PageTracker.set("   ");
        assertEquals("entries", PageTracker.current());
        PageTracker.reset();
        assertEquals("cover", PageTracker.current());
    }
}
