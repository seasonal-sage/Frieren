package com.timecapsule.analytics;

import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.core.Mood;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Reading-room statistics (Module: Analytics & Insights, Phase 2).
 * Everything derives from decrypted entries via Java Streams — no new
 * tables, no stored aggregates to go stale.
 */
public final class AnalyticsService {

    private static final Map<Mood, Double> WEIGHTS = Map.of(
            Mood.HAPPY, 2.0,
            Mood.CALM, 1.0,
            Mood.NEUTRAL, 0.0,
            Mood.SAD, -1.0,
            Mood.ANXIOUS, -2.0);

    private AnalyticsService() {
    }

    /** Mood tallies over the last {@code days} days (all moods present). */
    public static Map<Mood, Long> moodCounts(int days) {
        Map<Mood, Long> counts = entriesSince(days).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMood() == null ? Mood.NEUTRAL : m.getMood(),
                        () -> new EnumMap<>(Mood.class),
                        Collectors.counting()));
        for (Mood mood : Mood.values()) {
            counts.putIfAbsent(mood, 0L);
        }
        return counts;
    }

    /**
     * Weekly-style sentiment: average mood weight (-2 … +2).
     * {@code NaN} when nothing was written — "no data", not "neutral".
     */
    public static double sentimentScore(int days) {
        List<Memory> entries = entriesSince(days);
        if (entries.isEmpty()) {
            return Double.NaN;
        }
        return entries.stream()
                .mapToDouble(m -> WEIGHTS.getOrDefault(
                        m.getMood() == null ? Mood.NEUTRAL : m.getMood(), 0.0))
                .average()
                .orElse(Double.NaN);
    }

    /** Entries per day over the last {@code days} days (zero-filled). */
    public static Map<LocalDate, Long> entriesPerDay(int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (int i = days - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0L);
        }
        entriesSince(days).forEach(memory -> {
            LocalDate day = dayOf(memory);
            if (day != null) {
                perDay.merge(day, 1L, Long::sum);
            }
        });
        return perDay;
    }

    public static long totalEntries() {
        return JournalManager.listEntries().size();
    }

    private static List<Memory> entriesSince(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return JournalManager.listEntries().stream()
                .filter(m -> m.getCreatedAt() != null && !m.getCreatedAt().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    private static LocalDate dayOf(Memory memory) {
        if (memory.getCreatedAt() == null) {
            return null;
        }
        return memory.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
