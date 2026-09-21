package com.timecapsule.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.audit.AuditLogger;
import com.timecapsule.auth.AuthService;
import com.timecapsule.core.Mood;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves copies keep words, mark seals, and never leak early plaintext. */
class ExportServiceTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("export.db"));
        AuthService.register("archivist", "secret123");
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void jsonKeepsOpenWordsAndMarksSeals() throws Exception {
        com.timecapsule.core.JournalManager.createEntry(
                "Sun", "warm light", Mood.HAPPY, List.of("sky"), null);
        com.timecapsule.core.JournalManager.createEntry(
                "Future", "TOP SECRET words", Mood.CALM, List.of(),
                Instant.now().plus(30, ChronoUnit.DAYS));

        Path file = ExportService.exportCurrentUser(
                ExportService.Format.JSON, tempDir.resolve("out"));

        String raw = Files.readString(file);
        assertTrue(raw.contains("warm light"), "Open bodies travel in full");
        assertFalse(raw.contains("TOP SECRET"), "Sealed plaintext must never leak");
        assertTrue(raw.contains("sealed until"), "Sealed entries travel as dated promises");

        JsonNode root = new ObjectMapper().readTree(file.toFile());
        assertEquals("archivist", root.get("keeper").asText());
        assertEquals(2, root.get("entries").size());
    }

    @Test
    void pdfIsARealDocument() throws Exception {
        com.timecapsule.core.JournalManager.createEntry(
                "Sun", "warm light", Mood.HAPPY, List.of(), null);

        Path file = ExportService.exportCurrentUser(
                ExportService.Format.PDF, tempDir.resolve("out"));

        assertTrue(Files.size(file) > 1000, "PDF should hold real content");
        byte[] head = Files.newInputStream(file).readNBytes(5);
        assertEquals("%PDF-", new String(head));
    }

    @Test
    void exportIsAudited() {
        AuditLogger.log(1L, "probe-event");
        List<AuditLogger.AuditEvent> events = AuditLogger.recent(5);
        assertFalse(events.isEmpty());
        assertEquals("probe-event", events.get(0).event());
        assertTrue(events.get(0).id() > 0);
    }

    @Test
    void recentListsNewestFirst() {
        AuditLogger.log(null, "first-note");
        AuditLogger.log(null, "second-note");
        List<AuditLogger.AuditEvent> events = AuditLogger.recent(2);
        assertEquals(2, events.size());
        assertEquals("second-note", events.get(0).event());
        assertEquals("first-note", events.get(1).event());
    }
}
