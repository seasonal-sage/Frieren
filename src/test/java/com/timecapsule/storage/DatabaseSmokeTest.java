package com.timecapsule.storage;

import com.timecapsule.audit.AuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Boots a scratch database and proves the schema + audit path work. */
class DatabaseSmokeTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        DatabaseManager.shutdown();
    }

    @Test
    void freshDatabaseCreatesAllTables() throws Exception {
        DatabaseManager.useDatabaseFile(tempDir.resolve("smoke.db"));
        Connection conn = DatabaseManager.getConnection();

        Set<String> tables = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        assertTrue(tables.containsAll(Set.of(
                "users", "memories", "media", "habits", "habit_logs", "audit_logs")),
                "Missing tables, found: " + tables);
    }

    @Test
    void auditLogRoundTrip() throws Exception {
        DatabaseManager.useDatabaseFile(tempDir.resolve("audit.db"));
        AuditLogger.log(null, "smoke-test");

        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE event = ?")) {
            ps.setString(1, "smoke-test");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }
}
