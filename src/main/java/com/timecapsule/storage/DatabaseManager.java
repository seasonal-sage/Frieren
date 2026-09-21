package com.timecapsule.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * SQLite access (Module: Data Export & Persistence foundation).
 * App-scoped singleton connection; the database file lives in the user's
 * home ({@code ~/.timecapsule/timecapsule.db}) so it survives reinstalls.
 * The schema is applied automatically on first launch. Callers must NEVER
 * close the returned connection — use {@link #shutdown()} at exit.
 */
public final class DatabaseManager {

    private static final String APP_DIR = ".timecapsule";
    private static final String DB_NAME = "timecapsule.db";
    private static final String SCHEMA_PATH = "/com/timecapsule/schema.sql";

    private static Connection connection;
    private static Path overrideFile;

    private DatabaseManager() {
    }

    /** Returns the live connection, opening and migrating on first use. */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || isClosedQuietly()) {
            connection = open(databaseFile());
        }
        return connection;
    }

    /** Resolves the database file (test override wins over the home dir). */
    public static synchronized Path databaseFile() {
        if (overrideFile != null) {
            return overrideFile;
        }
        return Path.of(System.getProperty("user.home"), APP_DIR, DB_NAME);
    }

    /**
     * Points the manager at a different file (tests). Drops any open
     * connection so the next {@link #getConnection()} re-opens lazily.
     */
    public static synchronized void useDatabaseFile(Path file) {
        shutdown();
        overrideFile = file;
    }

    /** Closes the connection and clears any test override. */
    public static synchronized void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Best effort on shutdown.
            }
            connection = null;
        }
        overrideFile = null;
    }

    private static Connection open(Path file) throws SQLException {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new SQLException("Cannot create data directory: " + file.getParent(), e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        applySchema(conn);
        return conn;
    }

    private static void applySchema(Connection conn) throws SQLException {
        String script = readSchema();
        try (Statement st = conn.createStatement()) {
            for (String raw : script.split(";")) {
                String sql = raw.strip();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream in = DatabaseManager.class.getResourceAsStream(SCHEMA_PATH)) {
            if (in == null) {
                throw new SQLException("Schema resource missing: " + SCHEMA_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new SQLException("Cannot read schema resource", e);
        }
    }

    private static boolean isClosedQuietly() {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}
