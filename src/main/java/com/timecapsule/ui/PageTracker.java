package com.timecapsule.ui;

/**
 * Remembers which spread the keeper is looking at, so Frieren's
 * conversation always has context. Updated by every view on show —
 * cheap, synchronous, and UI-free (fully unit-testable).
 */
public final class PageTracker {

    private static String current = "cover";

    private PageTracker() {
    }

    public static void set(String pageId) {
        if (pageId != null && !pageId.isBlank()) {
            current = pageId.strip();
        }
    }

    public static String current() {
        return current;
    }

    static void reset() {
        current = "cover";
    }
}
