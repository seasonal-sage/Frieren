package com.timecapsule.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encrypted memory rows (Module: Memory Core persistence). Crypto-agnostic:
 * ciphertext goes in and out untouched — only {@code JournalManager} holds
 * keys. Every query is scoped by {@code user_id} (no cross-keeper reads).
 */
public class MemoryDao {

    /** One database row, body still sealed. */
    public record MemoryRow(long id, long userId, String title, byte[] cipher, byte[] iv,
                            String mood, String tags, boolean locked, Instant unlockDate,
                            Instant createdAt, Instant updatedAt) {
    }

    private static final String COLUMNS = "id, user_id, title, encrypted_body, iv, mood, tags,"
            + " is_locked, unlock_date, created_at, updated_at";

    public long insert(long userId, String title, byte[] cipher, byte[] iv, String mood,
                       String tags, boolean locked, Instant unlockDate) throws SQLException {
        String sql = "INSERT INTO memories (user_id, title, encrypted_body, iv, mood, tags,"
                + " is_locked, unlock_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setBytes(3, cipher);
            ps.setBytes(4, iv);
            ps.setString(5, mood);
            ps.setString(6, tags);
            ps.setInt(7, locked ? 1 : 0);
            setInstant(ps, 8, unlockDate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(long id, long userId, String title, byte[] cipher, byte[] iv, String mood,
                       String tags, boolean locked, Instant unlockDate) throws SQLException {
        String sql = "UPDATE memories SET title = ?, encrypted_body = ?, iv = ?, mood = ?,"
                + " tags = ?, is_locked = ?, unlock_date = ?,"
                + " updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"
                + " WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setBytes(2, cipher);
            ps.setBytes(3, iv);
            ps.setString(4, mood);
            ps.setString(5, tags);
            ps.setInt(6, locked ? 1 : 0);
            setInstant(ps, 7, unlockDate);
            ps.setLong(8, id);
            ps.setLong(9, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No such entry: " + id);
            }
        }
    }

    public boolean delete(long id, long userId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM memories WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Moves only the seal (flag + date), leaving ciphertext untouched — the
     * only safe edit for a locked entry whose body cannot be decrypted.
     */
    public void updateLock(long id, long userId, boolean locked, Instant unlockDate) throws SQLException {
        String sql = "UPDATE memories SET is_locked = ?, unlock_date = ?,"
                + " updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')"
                + " WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locked ? 1 : 0);
            setInstant(ps, 2, unlockDate);
            ps.setLong(3, id);
            ps.setLong(4, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No such entry: " + id);
            }
        }
    }

    public Optional<MemoryRow> findById(long id, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM memories WHERE id = ? AND user_id = ?";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<MemoryRow> listByUser(long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM memories WHERE user_id = ?"
                + " ORDER BY created_at DESC, id DESC";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MemoryRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    /** Sealed entries whose date has come (feeds the Day-5 scheduler). */
    public List<MemoryRow> listDue(long userId, Instant now) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM memories WHERE user_id = ? AND is_locked = 1"
                + " AND unlock_date IS NOT NULL AND unlock_date <= ? ORDER BY unlock_date";
        Connection conn = DatabaseManager.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, now.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<MemoryRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    private MemoryRow map(ResultSet rs) throws SQLException {
        String unlock = rs.getString("unlock_date");
        String created = rs.getString("created_at");
        String updated = rs.getString("updated_at");
        return new MemoryRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getBytes("encrypted_body"),
                rs.getBytes("iv"),
                rs.getString("mood"),
                rs.getString("tags"),
                rs.getInt("is_locked") == 1,
                unlock == null ? null : Instant.parse(unlock),
                created == null ? null : Instant.parse(created),
                updated == null ? null : Instant.parse(updated));
    }

    private void setInstant(PreparedStatement ps, int index, Instant value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value.toString());
        }
    }
}
