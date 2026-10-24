package com.timecapsule.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Frieren's long memory (Module: AI companion persistence). One row per
 * chat turn, scoped by keeper — Tier 1 of persistent memory (plaintext,
 * recent-N reload; embeddings stay a future enchantment).
 */
public class ChatDao {

    /** One stored turn (oldest first from {@link #recent}). */
    public record ChatRow(String role, String content) {
    }

    public long append(long userId, String role, String content) throws SQLException {
        String sql = "INSERT INTO chat_messages (user_id, role, content) VALUES (?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, role);
            ps.setString(3, content);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Latest turns, oldest-first, capped at {@code limit}. */
    public List<ChatRow> recent(long userId, int limit) throws SQLException {
        String sql = "SELECT role, content FROM chat_messages WHERE user_id = ?"
                + " ORDER BY id DESC LIMIT ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, Math.max(1, limit));
            List<ChatRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ChatRow(rs.getString("role"), rs.getString("content")));
                }
            }
            // Fetched newest-first for the LIMIT; callers read oldest-first.
            java.util.Collections.reverse(rows);
            return rows;
        }
    }

    /** Drops a keeper's transcript (fresh start, keeps other keepers). */
    public void clear(long userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM chat_messages WHERE user_id = ?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
