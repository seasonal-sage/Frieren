package com.timecapsule.storage;

import com.timecapsule.habits.Habit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Habit rows and daily logs (Module: Analytics persistence). Every query is
 * scoped by the keeper — logs are reachable only through owned habits.
 */
public class HabitDao {

    /** One habit row. */
    public record HabitRow(long id, long userId, String name, int targetPerWeek, Instant createdAt) {
    }

    public long create(long userId, String name, int targetPerWeek) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO habits (user_id, name, target_per_week) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setInt(3, targetPerWeek);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<HabitRow> listByUser(long userId) throws SQLException {
        String sql = "SELECT id, user_id, name, target_per_week, created_at FROM habits"
                + " WHERE user_id = ? ORDER BY id";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<HabitRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public Optional<HabitRow> findById(long id, long userId) throws SQLException {
        String sql = "SELECT id, user_id, name, target_per_week, created_at FROM habits"
                + " WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean delete(long id, long userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM habits WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Records a day (insert or overwrite) for an owned habit. */
    public void logDay(long habitId, long userId, LocalDate date, boolean done) throws SQLException {
        if (findById(habitId, userId).isEmpty()) {
            throw new SQLException("No such habit: " + habitId);
        }
        String sql = "INSERT INTO habit_logs (habit_id, log_date, done) VALUES (?, ?, ?)"
                + " ON CONFLICT (habit_id, log_date) DO UPDATE SET done = excluded.done";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, habitId);
            ps.setString(2, date.toString());
            ps.setInt(3, done ? 1 : 0);
            ps.executeUpdate();
        }
    }

    /** Done-dates for a habit since (inclusive) the given day. */
    public Set<LocalDate> doneDatesSince(long habitId, long userId, LocalDate since) throws SQLException {
        if (findById(habitId, userId).isEmpty()) {
            throw new SQLException("No such habit: " + habitId);
        }
        String sql = "SELECT log_date FROM habit_logs WHERE habit_id = ? AND done = 1 AND log_date >= ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, habitId);
            ps.setString(2, since.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Set<LocalDate> dates = new HashSet<>();
                while (rs.next()) {
                    dates.add(LocalDate.parse(rs.getString("log_date")));
                }
                return dates;
            }
        }
    }

    private HabitRow map(ResultSet rs) throws SQLException {
        String created = rs.getString("created_at");
        return new HabitRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getInt("target_per_week"),
                created == null ? null : Instant.parse(created));
    }

    public static Habit toHabit(HabitRow row) {
        return new Habit(row.id(), row.userId(), row.name(), row.targetPerWeek(), row.createdAt());
    }
}
