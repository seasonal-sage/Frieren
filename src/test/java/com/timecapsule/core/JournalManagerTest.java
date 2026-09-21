package com.timecapsule.core;

import com.timecapsule.auth.AuthService;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves CRUD, lock enforcement, and keeper isolation on a scratch database. */
class JournalManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("journal.db"));
        AuthService.register("chronicler", "secret123");
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void createAndReadRoundTrip() {
        Memory created = JournalManager.createEntry(
                "First snow", "It fell softly.", Mood.CALM, List.of("winter", "home"), null);
        assertFalse(created.isLocked());
        assertEquals("It fell softly.", created.getBody());
        assertEquals(List.of("winter", "home"), created.getTags());

        Memory read = JournalManager.getEntry(created.getId());
        assertEquals("First snow", read.getTitle());
        assertEquals(Mood.CALM, read.getMood());
    }

    @Test
    void futureEntrySealsItsBody() {
        Memory sealed = JournalManager.createEntry(
                "To 2030", "Dear future me.", Mood.HAPPY, List.of(),
                Instant.now().plus(365, ChronoUnit.DAYS));
        assertTrue(sealed.isLocked());
        assertNull(sealed.getBody(), "Sealed body must never decrypt early");
        assertEquals("To 2030", sealed.getTitle(), "Metadata stays visible");

        Memory fetched = JournalManager.getEntry(sealed.getId());
        assertNull(fetched.getBody());
    }

    @Test
    void pastUnlockDateReadsOpen() {
        Memory open = JournalManager.createEntry(
                "Old seal", "The date has come.", Mood.NEUTRAL, List.of(),
                Instant.now().minus(1, ChronoUnit.HOURS));
        // Still flagged as a capsule, but the seal has lifted: body reads open.
        assertTrue(open.isLocked());
        assertTrue(open.isUnlockedAt(Instant.now()));
        assertEquals("The date has come.", open.getBody());
    }

    @Test
    void dueEntriesFeedTheScheduler() {
        JournalManager.createEntry("Soon", "Almost.", Mood.HAPPY, List.of(),
                Instant.now().minus(1, ChronoUnit.MINUTES));
        JournalManager.createEntry("Later", "Not yet.", Mood.HAPPY, List.of(),
                Instant.now().plus(30, ChronoUnit.DAYS));
        JournalManager.createEntry("Plain", "No seal.", Mood.HAPPY, List.of(), null);

        List<Memory> due = JournalManager.listDueEntries();
        assertEquals(1, due.size());
        assertEquals("Soon", due.get(0).getTitle());
    }

    @Test
    void updateAndDelete() {
        Memory created = JournalManager.createEntry(
                "Draft", "v1", Mood.SAD, List.of("a"), null);
        Memory updated = JournalManager.updateEntry(
                created.getId(), "Polished", "v2", Mood.HAPPY, List.of("a", "b"), null);
        assertEquals("Polished", updated.getTitle());
        assertEquals("v2", updated.getBody());

        JournalManager.deleteEntry(created.getId());
        assertThrows(JournalException.class, () -> JournalManager.getEntry(created.getId()));
    }

    @Test
    void keepersCannotSeeEachOthersPages() {
        Memory mine = JournalManager.createEntry(
                "Mine", "Private.", Mood.CALM, List.of(), null);
        AuthService.logout();
        AuthService.register("stranger", "otherpass1");

        assertTrue(JournalManager.listEntries().isEmpty());
        assertThrows(JournalException.class, () -> JournalManager.getEntry(mine.getId()));
        assertThrows(JournalException.class,
                () -> JournalManager.updateEntry(mine.getId(), "X", "Y", null, List.of(), null));
    }

    @Test
    void blankTitlesAreRejected() {
        assertThrows(JournalException.class,
                () -> JournalManager.createEntry("   ", "body", null, List.of(), null));
    }

    @Test
    void unsignedKeepersAreTurnedAway() {
        AuthService.logout();
        assertThrows(JournalException.class,
                () -> JournalManager.createEntry("T", "B", null, List.of(), null));
    }
}
