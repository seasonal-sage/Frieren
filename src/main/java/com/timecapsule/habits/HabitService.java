package com.timecapsule.habits;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import com.timecapsule.core.JournalException;
import com.timecapsule.storage.HabitDao;
import com.timecapsule.storage.HabitDao.HabitRow;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Habit practice (Module: Analytics & Insights, Phase 2). Streaks count
 * consecutive done-days ending today — or yesterday, so a keeper mid-day
 * is never punished for not having logged yet.
 */
public final class HabitService {

    private static final HabitDao HABITS = new HabitDao();

    private HabitService() {
    }

    public static Habit createHabit(String name, int targetPerWeek) {
        User user = requireUser();
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty() || clean.length() > 80) {
            throw new JournalException("Practices need a name (1–80 letters).");
        }
        if (targetPerWeek < 1 || targetPerWeek > 7) {
            throw new JournalException("Weekly aims run 1 to 7 days.");
        }
        try {
            long id = HABITS.create(user.getId(), clean, targetPerWeek);
            AuditLogger.log(user.getId(), "habit.create " + id);
            return HabitDao.toHabit(HABITS.findById(id, user.getId())
                    .orElseThrow(() -> new JournalException("That practice would not take.")));
        } catch (JournalException e) {
            throw e;
        } catch (SQLException e) {
            throw new JournalException("That practice would not take.", e);
        }
    }

    public static List<Habit> listHabits() {
        User user = requireUser();
        try {
            return HABITS.listByUser(user.getId()).stream()
                    .map(HabitDao::toHabit)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new JournalException("Could not open the practice page.", e);
        }
    }

    public static void deleteHabit(long habitId) {
        User user = requireUser();
        try {
            if (!HABITS.delete(habitId, user.getId())) {
                throw new JournalException("No practice answers to that number.");
            }
            AuditLogger.log(user.getId(), "habit.delete " + habitId);
        } catch (JournalException e) {
            throw e;
        } catch (SQLException e) {
            throw new JournalException("That practice would not tear out.", e);
        }
    }

    public static void logToday(long habitId, boolean done) {
        logDate(habitId, LocalDate.now(), done);
    }

    public static void logDate(long habitId, LocalDate date, boolean done) {
        User user = requireUser();
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new JournalException("Only today and past days can be marked.");
        }
        try {
            HABITS.logDay(habitId, user.getId(), date, done);
            AuditLogger.log(user.getId(), "habit.log " + habitId + " " + date);
        } catch (SQLException e) {
            throw new JournalException("That mark would not stick.", e);
        }
    }

    public static boolean isDoneOn(long habitId, LocalDate date) {
        User user = requireUser();
        try {
            return HABITS.doneDatesSince(habitId, user.getId(), date).contains(date);
        } catch (SQLException e) {
            throw new JournalException("Could not read the marks.", e);
        }
    }

    /** Consecutive done-days ending today (or yesterday if today is unmarked). */
    public static int streak(long habitId) {
        User user = requireUser();
        try {
            Set<LocalDate> done = HABITS.doneDatesSince(
                    habitId, user.getId(), LocalDate.now().minusDays(400));
            LocalDate cursor = LocalDate.now();
            if (!done.contains(cursor)) {
                cursor = cursor.minusDays(1);
            }
            int streak = 0;
            while (done.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
            return streak;
        } catch (SQLException e) {
            throw new JournalException("Could not count the streak.", e);
        }
    }

    /** Done-days in the last 7 days (today included). */
    public static int weekCount(long habitId) {
        User user = requireUser();
        try {
            Set<LocalDate> done = HABITS.doneDatesSince(
                    habitId, user.getId(), LocalDate.now().minusDays(6));
            int count = 0;
            for (int i = 0; i < 7; i++) {
                if (done.contains(LocalDate.now().minusDays(i))) {
                    count++;
                }
            }
            return count;
        } catch (SQLException e) {
            throw new JournalException("Could not count the week.", e);
        }
    }

    private static User requireUser() {
        User user = AuthService.currentUser();
        if (user == null) {
            throw new JournalException("No keeper is signed in.");
        }
        return user;
    }
}
