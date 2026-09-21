package com.timecapsule.habits;

import com.timecapsule.auth.AuthService;
import com.timecapsule.core.JournalException;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves streaks, weeks, validation, and keeper isolation for habits. */
class HabitServiceTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("habits.db"));
        AuthService.register("ascetic", "secret123");
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void createAndList() {
        Habit habit = HabitService.createHabit("Morning pages", 5);
        assertEquals("Morning pages", habit.getName());
        assertEquals(5, habit.getTargetPerWeek());
        assertEquals(1, HabitService.listHabits().size());
    }

    @Test
    void streakCountsBackThroughToday() {
        Habit habit = HabitService.createHabit("Tea", 7);
        LocalDate today = LocalDate.now();
        HabitService.logDate(habit.getId(), today, true);
        HabitService.logDate(habit.getId(), today.minusDays(1), true);
        HabitService.logDate(habit.getId(), today.minusDays(2), true);
        assertEquals(3, HabitService.streak(habit.getId()));
    }

    @Test
    void unmarkedTodayStillCountsFromYesterday() {
        Habit habit = HabitService.createHabit("Walk", 7);
        LocalDate today = LocalDate.now();
        HabitService.logDate(habit.getId(), today.minusDays(1), true);
        HabitService.logDate(habit.getId(), today.minusDays(2), true);
        assertEquals(2, HabitService.streak(habit.getId()));
    }

    @Test
    void gapsBreakStreaks() {
        Habit habit = HabitService.createHabit("Read", 7);
        LocalDate today = LocalDate.now();
        HabitService.logDate(habit.getId(), today, true);
        HabitService.logDate(habit.getId(), today.minusDays(2), true);
        assertEquals(1, HabitService.streak(habit.getId()));

        HabitService.logDate(habit.getId(), today, false);
        assertEquals(0, HabitService.streak(habit.getId()));
    }

    @Test
    void weekCountsLastSevenDays() {
        Habit habit = HabitService.createHabit("Meditate", 3);
        LocalDate today = LocalDate.now();
        HabitService.logDate(habit.getId(), today, true);
        HabitService.logDate(habit.getId(), today.minusDays(3), true);
        HabitService.logDate(habit.getId(), today.minusDays(9), true);
        assertEquals(2, HabitService.weekCount(habit.getId()));
    }

    @Test
    void futureMarksAndBadAimsRejected() {
        Habit habit = HabitService.createHabit("Stretch", 2);
        assertThrows(JournalException.class, () ->
                HabitService.logDate(habit.getId(), LocalDate.now().plusDays(1), true));
        assertThrows(JournalException.class, () -> HabitService.createHabit("X", 0));
        assertThrows(JournalException.class, () -> HabitService.createHabit("   ", 3));
    }

    @Test
    void deleteAndIsolation() {
        Habit habit = HabitService.createHabit("Mine", 1);
        HabitService.deleteHabit(habit.getId());
        assertTrue(HabitService.listHabits().isEmpty());

        Habit kept = HabitService.createHabit("Kept", 1);
        AuthService.logout();
        AuthService.register("prowler", "otherpass1");
        assertTrue(HabitService.listHabits().isEmpty());
        assertThrows(JournalException.class, () -> HabitService.streak(kept.getId()));
    }

    @Test
    void unsignedKeepersTurnedAway() {
        AuthService.logout();
        assertThrows(JournalException.class, HabitService::listHabits);
    }
}
