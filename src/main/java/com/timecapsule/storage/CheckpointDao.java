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
 * Keeper goals and milestones (Module: Self-Improvement). A checkpoint is a
 * promise to yourself — a goal with an optional target date that you mark
 * complete when you reach it. Keeper-scoped, never shared.
 */
public class CheckpointDao {

    /** One checkpoint row. */
    public record CheckpointRow(long id, long userId, String title,
                                String description, LocalDate targetDate,
                                String status, String createdAt, String completedAt) {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Creates a new pending checkpoint. */
    public long insert(long userId, String title, String description,
                       LocalDate targetDate) throws SQLException {
        String sql = "INSERT INTO checkpoints (user_id, title, description,"
                + " target_date, status) VALUES (?, ?, ?, ?, 'pending')";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            if (description == null) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, description);
            }
            if (targetDate == null) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, targetDate.format(DATE_FMT));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** All checkpoints for a keeper, newest first. */
    public List<CheckpointRow> listAll(long userId) throws SQLException {
        return listByStatus(userId, null);
    }

    /** Pending (active) checkpoints, newest first. */
    public List<CheckpointRow> listPending(long userId) throws SQLException {
        return listByStatus(userId, "pending");
    }

    /** Completed checkpoints, newest first by completion date. */
    public List<CheckpointRow> listCompleted(long userId) throws SQLException {
        return listByStatus(userId, "completed");
    }

    private List<CheckpointRow> listByStatus(long userId, String status) throws SQLException {
        String sql = "SELECT id, user_id, title, description, target_date,"
                + " status, created_at, completed_at FROM checkpoints"
                + " WHERE user_id = ?" + (status == null ? "" : " AND status = ?")
                + " ORDER BY created_at DESC, id DESC";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            if (status != null) {
                ps.setString(2, status);
            }
            List<CheckpointRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(map(rs));
                }
            }
            return rows;
        }
    }

    /** Marks a checkpoint complete and stamps completed_at. */
    public void markComplete(long id, long userId) throws SQLException {
        String sql = "UPDATE checkpoints SET status = 'completed',"
                + " completed_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"
                + " WHERE id = ? AND user_id = ? AND status != 'completed'";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /** Reopens a completed checkpoint (moves back to pending). */
    public void markPending(long id, long userId) throws SQLException {
        String sql = "UPDATE checkpoints SET status = 'pending', completed_at = NULL"
                + " WHERE id = ? AND user_id = ? AND status != 'pending'";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /** Edits a checkpoint (keeper-scoped). */
    public void update(long id, long userId, String title, String description,
                       LocalDate targetDate) throws SQLException {
        String sql = "UPDATE checkpoints SET title = ?, description = ?,"
                + " target_date = ? WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            if (description == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, description);
            }
            if (targetDate == null) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, targetDate.format(DATE_FMT));
            }
            ps.setLong(4, id);
            ps.setLong(5, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No such checkpoint: " + id);
            }
        }
    }

    /** Deletes a checkpoint (keeper-scoped). */
    public boolean delete(long id, long userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM checkpoints WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Counts completed checkpoints (for stats). */
    public long countCompleted(long userId) throws SQLException {
        return countByStatus(userId, "completed");
    }

    /** Counts pending checkpoints (for stats). */
    public long countPending(long userId) throws SQLException {
        return countByStatus(userId, "pending");
    }

    private long countByStatus(long userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM checkpoints WHERE user_id = ? AND status = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private CheckpointRow map(ResultSet rs) throws SQLException {
        String targetStr = rs.getString("target_date");
        return new CheckpointRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getString("description"),
                targetStr == null ? null : LocalDate.parse(targetStr),
                rs.getString("status"),
                rs.getString("created_at"),
                rs.getString("completed_at"));
    }
}
