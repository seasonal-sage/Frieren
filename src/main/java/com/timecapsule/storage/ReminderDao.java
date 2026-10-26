package com.timecapsule.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeper reminders (Module: Calendar). One row per reminder, scoped by
 * keeper. Supports one-time plus daily / weekly / monthly repetition.
 * Reminders surface only when the app opens — they never nag mid-session.
 */
public class ReminderDao {

    /** One reminder row. */
    public record ReminderRow(long id, long userId, LocalDate reminderDate,
                              String message, Long entryId, String repeatType,
                              boolean fired, String createdAt) {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Creates a reminder. Defaults to a one-time reminder on {@code reminderDate}. */
    public long insert(long userId, LocalDate reminderDate, String message,
                       Long entryId, String repeatType) throws SQLException {
        String sql = "INSERT INTO reminders (user_id, reminder_date, message,"
                + " entry_id, repeat_type) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, reminderDate.format(DATE_FMT));
            ps.setString(3, message);
            if (entryId == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setLong(4, entryId);
            }
            ps.setString(5, repeatType == null ? "none" : repeatType);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /**
     * Due reminders for today. One-time reminders must be unfired;
     * repeating ones are due only on their occurrence dates
     * (daily: every day; weekly: every 7 days; monthly: same day-of-month).
     * Returns oldest-first.
     */
    public List<ReminderRow> listDue(long userId, LocalDate today) throws SQLException {
        String sql = "SELECT id, user_id, reminder_date, message, entry_id,"
                + " repeat_type, fired, created_at FROM reminders"
                + " WHERE user_id = ? AND reminder_date <= ? ORDER BY reminder_date, id";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, today.format(DATE_FMT));
            List<ReminderRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String repeat = rs.getString("repeat_type");
                    boolean fired = rs.getInt("fired") == 1;
                    LocalDate start = LocalDate.parse(rs.getString("reminder_date"));
                    // One-time reminders that already fired are not due.
                    if ("none".equals(repeat) && fired) {
                        continue;
                    }
                    // Repeating reminders: only due on occurrence dates.
                    if ("daily".equals(repeat)) {
                        // Due every day from start onward — always true since start <= today.
                    } else if ("weekly".equals(repeat)) {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, today);
                        if (daysBetween % 7 != 0) {
                            continue;
                        }
                    } else if ("monthly".equals(repeat)) {
                        if (today.getDayOfMonth() != start.getDayOfMonth()) {
                            continue;
                        }
                    }
                    rows.add(map(rs));
                }
            }
            return rows;
        }
    }

    /** All upcoming (not yet fired) reminders, oldest-first. */
    public List<ReminderRow> listUpcoming(long userId) throws SQLException {
        String sql = "SELECT id, user_id, reminder_date, message, entry_id,"
                + " repeat_type, fired, created_at FROM reminders"
                + " WHERE user_id = ? AND fired = 0 ORDER BY reminder_date, id";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            List<ReminderRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(map(rs));
                }
            }
            return rows;
        }
    }

    /** Marks a one-time reminder as fired. No-op for repeating reminders. */
    public void markFired(long id) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE reminders SET fired = 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Advances a repeating reminder to its next occurrence date. */
    public void advance(long id, String repeatType, LocalDate currentDate) throws SQLException {
        LocalDate next;
        switch (repeatType == null ? "none" : repeatType) {
            case "daily":
                next = currentDate.plusDays(1);
                break;
            case "weekly":
                next = currentDate.plusWeeks(1);
                break;
            case "monthly":
                next = currentDate.plusMonths(1);
                break;
            default:
                return; // One-time — nothing to advance.
        }
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE reminders SET reminder_date = ? WHERE id = ?")) {
            ps.setString(1, next.format(DATE_FMT));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Edits a reminder (keeper-scoped). */
    public void update(long id, long userId, LocalDate reminderDate, String message,
                       Long entryId, String repeatType) throws SQLException {
        String sql = "UPDATE reminders SET reminder_date = ?, message = ?,"
                + " entry_id = ?, repeat_type = ? WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reminderDate.format(DATE_FMT));
            ps.setString(2, message);
            if (entryId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setLong(3, entryId);
            }
            ps.setString(4, repeatType == null ? "none" : repeatType);
            ps.setLong(5, id);
            ps.setLong(6, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No such reminder: " + id);
            }
        }
    }

    /** Deletes a reminder (keeper-scoped). */
    public boolean delete(long id, long userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM reminders WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private ReminderRow map(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("reminder_date");
        Long entryId;
        try {
            entryId = rs.getObject("entry_id", Long.class);
        } catch (SQLException e) {
            entryId = null;
        }
        return new ReminderRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                dateStr == null ? null : LocalDate.parse(dateStr),
                rs.getString("message"),
                entryId,
                rs.getString("repeat_type"),
                rs.getInt("fired") == 1,
                rs.getString("created_at"));
    }
}
