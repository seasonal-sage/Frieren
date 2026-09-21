package com.timecapsule.storage;

import com.timecapsule.media.Media;
import com.timecapsule.media.MediaFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Attachment rows (Module: Media Processing persistence). Keeper scope is
 * enforced with a join — a row is reachable only through its owner's entry.
 */
public class MediaDao {

    /** One attachment row. */
    public record MediaRow(long id, long memoryId, String type, String filePath, Instant createdAt) {
    }

    private static final String COLUMNS = "m.id, m.memory_id, m.type, m.file_path, m.created_at";

    public long insert(long memoryId, String type, String filePath) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO media (memory_id, type, file_path) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, memoryId);
            ps.setString(2, type);
            ps.setString(3, filePath);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public List<MediaRow> listByMemory(long memoryId, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM media m JOIN memories e ON e.id = m.memory_id"
                + " WHERE m.memory_id = ? AND e.user_id = ? ORDER BY m.id";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memoryId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MediaRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public Optional<MediaRow> findById(long id, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM media m JOIN memories e ON e.id = m.memory_id"
                + " WHERE m.id = ? AND e.user_id = ?";
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
        String sql = "DELETE FROM media WHERE id = ? AND memory_id IN"
                + " (SELECT id FROM memories WHERE user_id = ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private MediaRow map(ResultSet rs) throws SQLException {
        String created = rs.getString("created_at");
        return new MediaRow(
                rs.getLong("id"),
                rs.getLong("memory_id"),
                rs.getString("type"),
                rs.getString("file_path"),
                created == null ? null : Instant.parse(created));
    }

    /** Rebuilds the domain object from a row. */
    public static Media toMedia(MediaRow row) {
        return MediaFactory.fromRow(
                row.id(), row.memoryId(), row.type(), Path.of(row.filePath()), row.createdAt());
    }
}
