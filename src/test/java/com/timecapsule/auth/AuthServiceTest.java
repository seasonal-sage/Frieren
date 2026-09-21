package com.timecapsule.auth;

import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Proves register/login/session rules on a scratch database. */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("auth.db"));
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void registerSignsStraightIn() {
        User user = AuthService.register("keeper1", "secret123");
        assertEquals("keeper1", user.getUsername());
        assertEquals(user.getId(), AuthService.currentUser().getId());
        assertNotNull(AuthService.sessionKey());
    }

    @Test
    void loginAfterLogout() {
        AuthService.register("keeper2", "secret123");
        AuthService.logout();
        assertNull(AuthService.currentUser());
        User again = AuthService.login("keeper2", "secret123");
        assertEquals("keeper2", again.getUsername());
    }

    @Test
    void wrongPasswordIsRejected() {
        AuthService.register("keeper3", "secret123");
        AuthService.logout();
        assertThrows(AuthException.class, () -> AuthService.login("keeper3", "nope-nope"));
    }

    @Test
    void unknownKeeperIsRejected() {
        assertThrows(AuthException.class, () -> AuthService.login("nobody", "whatever1"));
    }

    @Test
    void duplicateNamesAreRejected() {
        AuthService.register("keeper4", "secret123");
        assertThrows(AuthException.class, () -> AuthService.register("keeper4", "otherpass"));
    }

    @Test
    void weakSecretsAreRejected() {
        assertThrows(AuthException.class, () -> AuthService.register("ok", "secret123"));
        assertThrows(AuthException.class, () -> AuthService.register("keeper5", "short"));
    }
}
