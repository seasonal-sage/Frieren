package com.timecapsule.audit;

import com.timecapsule.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Local event recorder (Module: Audit & Diagnostics Logger, minimal
 * Session-A slice). Appends to {@code audit_logs}; never throws — logging
 * must not break the feature being logged.
 */
public final class AuditLogger {

    private AuditLogger() {
    }

    /** Records an event; {@code userId} may be null for pre-login events. */
    public static void log(Long userId, String event) {
        try {
            Connection conn = DatabaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO audit_logs (user_id, event) VALUES (?, ?)")) {
                if (userId == null) {
                    ps.setNull(1, Types.INTEGER);
                } else {
                    ps.setLong(1, userId);
                }
                ps.setString(2, event);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[audit] " + event + " (" + e.getMessage() + ")");
        }
    }

    /** One recorded event for the librarian's notes. */
    public record AuditEvent(long id, Long userId, String event, String createdAt) {
    }

    /** Newest-first recent events (never throws — an empty shelf on failure). */
    public static List<AuditEvent> recent(int limit) {
        List<AuditEvent> events = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, user_id, event, created_at FROM audit_logs"
                            + " ORDER BY id DESC LIMIT ?")) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long userId = rs.getLong("user_id");
                        events.add(new AuditEvent(rs.getLong("id"),
                                rs.wasNull() ? null : userId,
                                rs.getString("event"),
                                rs.getString("created_at")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[audit] cannot read notes (" + e.getMessage() + ")");
        }
        return events;
    }
}
