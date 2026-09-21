package com.timecapsule.analytics;

import com.timecapsule.auth.AuthService;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Mood;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves mood tallies, sentiment maths, and per-day counts. */
class AnalyticsServiceTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("stats.db"));
        AuthService.register("statistician", "secret123");
        JournalManager.createEntry("A", "x", Mood.HAPPY, List.of(), null);
        JournalManager.createEntry("B", "x", Mood.CALM, List.of(), null);
        JournalManager.createEntry("C", "x", Mood.NEUTRAL, List.of(), null);
        JournalManager.createEntry("D", "x", Mood.SAD, List.of(), null);
        JournalManager.createEntry("E", "x", Mood.ANXIOUS, List.of(), null);
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void moodCountsCoverAllMoods() {
        Map<Mood, Long> counts = AnalyticsService.moodCounts(30);
        for (Mood mood : Mood.values()) {
            assertEquals(1L, counts.get(mood));
        }
    }

    @Test
    void sentimentAveragesWeights() {
        // (2 + 1 + 0 - 1 - 2) / 5 = 0
        assertEquals(0.0, AnalyticsService.sentimentScore(30), 0.0001);
    }

    @Test
    void perDayCountsToday() {
        Map<LocalDate, Long> perDay = AnalyticsService.entriesPerDay(7);
        assertEquals(7, perDay.size(), "Range is zero-filled");
        assertEquals(5L, perDay.get(LocalDate.now()));
    }

    @Test
    void emptyDiaryHasNoSentiment() {
        AuthService.logout();
        AuthService.register("blank", "otherpass1");
        assertTrue(Double.isNaN(AnalyticsService.sentimentScore(30)));
        assertEquals(0L, AnalyticsService.totalEntries());
    }
}
