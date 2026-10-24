package com.timecapsule.storage;

import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves Frieren's long memory persists per keeper on a scratch database. */
class ChatDaoTest {

    @TempDir
    Path tempDir;

    private final ChatDao chats = new ChatDao();
    private User keeper;
    private User other;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("chat.db"));
        keeper = AuthService.register("dreamer", "secret123");
        other = AuthService.register("sleeper", "secret123");
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void recentReturnsOldestFirst() throws Exception {
        chats.append(keeper.getId(), "user", "hello");
        chats.append(keeper.getId(), "assistant", "well met");
        List<ChatDao.ChatRow> rows = chats.recent(keeper.getId(), 20);
        assertEquals(2, rows.size());
        assertEquals("hello", rows.get(0).content());
        assertEquals("well met", rows.get(1).content());
    }

    @Test
    void limitKeepsLatest() throws Exception {
        for (int i = 1; i <= 5; i++) {
            chats.append(keeper.getId(), "user", "line " + i);
        }
        List<ChatDao.ChatRow> rows = chats.recent(keeper.getId(), 2);
        assertEquals(List.of(
                new ChatDao.ChatRow("user", "line 4"),
                new ChatDao.ChatRow("user", "line 5")), rows);
    }

    @Test
    void keepersCannotHearEachOther() throws Exception {
        chats.append(keeper.getId(), "user", "mine");
        assertTrue(chats.recent(other.getId(), 20).isEmpty());
        chats.clear(keeper.getId());
        chats.append(other.getId(), "user", "theirs");
        assertTrue(chats.recent(keeper.getId(), 20).isEmpty());
    }
}
