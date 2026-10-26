package com.timecapsule.storage;

import com.timecapsule.auth.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves keeper reminders: one-time, daily, weekly, monthly, and fire-on-open. */
class ReminderDaoTest {

    @TempDir
    Path tempDir;

    private final ReminderDao reminders = new ReminderDao();
    private long keeper;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.useDatabaseFile(tempDir.resolve("reminder.db"));
        keeper = AuthService.register("reminder-keeper", "secret123").getId();
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void oneTimeReminderDueOnDate() throws Exception {
        LocalDate today = LocalDate.now();
        reminders.insert(keeper, today, "Buy milk", null, "none");
        List<ReminderDao.ReminderRow> due = reminders.listDue(keeper, today);
        assertEquals(1, due.size());
        assertEquals("Buy milk", due.get(0).message());
    }

    @Test
    void oneTimeReminderNotDueBeforeDate() throws Exception {
        LocalDate today = LocalDate.now();
        reminders.insert(keeper, today.plusDays(5), "Future milk", null, "none");
        assertTrue(reminders.listDue(keeper, today).isEmpty());
    }

    @Test
    void oneTimeReminderMarkedFiredNotDueAgain() throws Exception {
        LocalDate today = LocalDate.now();
        long id = reminders.insert(keeper, today, "Once only", null, "none");
        reminders.listDue(keeper, today); // consume
        reminders.markFired(id);
        assertTrue(reminders.listDue(keeper, today).isEmpty());
    }

    @Test
    void dailyReminderDueEveryDay() throws Exception {
        LocalDate today = LocalDate.now();
        long id = reminders.insert(keeper, today, "Daily pills", null, "daily");
        assertEquals(1, reminders.listDue(keeper, today).size());
        assertEquals(1, reminders.listDue(keeper, today.plusDays(1)).size());
        assertEquals(1, reminders.listDue(keeper, today.plusDays(30)).size());
    }

    @Test
    void weeklyReminderDueEveryWeek() throws Exception {
        LocalDate today = LocalDate.now();
        long id = reminders.insert(keeper, today, "Weekly review", null, "weekly");
        assertEquals(1, reminders.listDue(keeper, today).size());
        assertTrue(reminders.listDue(keeper, today.plusDays(3)).isEmpty());
        assertEquals(1, reminders.listDue(keeper, today.plusDays(7)).size());
    }

    @Test
    void monthlyReminderDueSameDayEachMonth() throws Exception {
        LocalDate today = LocalDate.now().withDayOfMonth(15);
        long id = reminders.insert(keeper, today, "Monthly rent", null, "monthly");
        assertEquals(1, reminders.listDue(keeper, today).size());
        // Same day next month
        assertEquals(1, reminders.listDue(keeper, today.plusMonths(1)).size());
    }

    @Test
    void keepersCannotSeeEachOthersReminders() throws Exception {
        LocalDate today = LocalDate.now();
        reminders.insert(keeper, today, "My secret", null, "none");
        AuthService.logout();
        long other = AuthService.register("other-keeper", "otherpass1").getId();
        assertTrue(reminders.listDue(other, today).isEmpty());
    }

    @Test
    void advanceMovesToNextOccurrence() throws Exception {
        LocalDate today = LocalDate.now();
        long id = reminders.insert(keeper, today, "Advance me", null, "weekly");
        reminders.advance(id, "weekly", today);
        // After advancing, the old date is past so it shouldn't show as due again immediately
        List<ReminderDao.ReminderRow> due = reminders.listDue(keeper, today);
        assertTrue(due.isEmpty() || !due.get(0).reminderDate().equals(today));
    }
}
