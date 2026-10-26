package com.timecapsule.storage;

import com.timecapsule.auth.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves keeper checkpoints: create, complete, reopen, edit, delete, isolate. */
class CheckpointDaoTest {

    @TempDir
    Path tempDir;

    private final CheckpointDao checkpoints = new CheckpointDao();
    private long keeper;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.useDatabaseFile(tempDir.resolve("checkpoint.db"));
        keeper = AuthService.register("goal-keeper", "secret123").getId();
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void createAndListPending() throws Exception {
        long id = checkpoints.insert(keeper, "Read a book", "Finish one novel", LocalDate.now().plusDays(7));
        List<CheckpointDao.CheckpointRow> pending = checkpoints.listPending(keeper);
        assertEquals(1, pending.size());
        assertEquals("Read a book", pending.get(0).title());
        assertEquals("pending", pending.get(0).status());
    }

    @Test
    void markCompleteMovesToCompleted() throws Exception {
        long id = checkpoints.insert(keeper, "Morning run", null, null);
        checkpoints.markComplete(id, keeper);
        List<CheckpointDao.CheckpointRow> completed = checkpoints.listCompleted(keeper);
        assertEquals(1, completed.size());
        assertEquals("completed", completed.get(0).status());
        assertTrue(completed.get(0).completedAt() != null);
        assertTrue(checkpoints.listPending(keeper).isEmpty());
    }

    @Test
    void markPendingReopens() throws Exception {
        long id = checkpoints.insert(keeper, "Reopen me", null, null);
        checkpoints.markComplete(id, keeper);
        checkpoints.markPending(id, keeper);
        assertEquals(1, checkpoints.listPending(keeper).size());
        assertTrue(checkpoints.listCompleted(keeper).isEmpty());
    }

    @Test
    void editCheckpoint() throws Exception {
        long id = checkpoints.insert(keeper, "Old title", "Old desc", LocalDate.of(2026, 1, 1));
        checkpoints.update(id, keeper, "New title", "New desc", LocalDate.of(2026, 2, 2));
        List<CheckpointDao.CheckpointRow> all = checkpoints.listAll(keeper);
        assertEquals(1, all.size());
        assertEquals("New title", all.get(0).title());
        assertEquals("New desc", all.get(0).description());
        assertEquals(LocalDate.of(2026, 2, 2), all.get(0).targetDate());
    }

    @Test
    void deleteCheckpoint() throws Exception {
        long id = checkpoints.insert(keeper, "To be deleted", null, null);
        assertTrue(checkpoints.delete(id, keeper));
        assertTrue(checkpoints.listAll(keeper).isEmpty());
    }

    @Test
    void keepersCannotSeeEachOthersCheckpoints() throws Exception {
        checkpoints.insert(keeper, "My goal", null, null);
        AuthService.logout();
        long other = AuthService.register("other-keeper", "otherpass1").getId();
        assertTrue(checkpoints.listAll(other).isEmpty());
    }

    @Test
    void countsForStats() throws Exception {
        checkpoints.insert(keeper, "Done one", null, null);
        checkpoints.insert(keeper, "Done two", null, null);
        checkpoints.markComplete(checkpoints.listPending(keeper).get(0).id(), keeper);
        checkpoints.markComplete(checkpoints.listPending(keeper).get(0).id(), keeper);
        assertEquals(2, checkpoints.countCompleted(keeper));
        assertEquals(0, checkpoints.countPending(keeper));
    }
}
