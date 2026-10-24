package com.timecapsule.media;

import com.timecapsule.auth.AuthService;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves vault copy, listing, removal, and keeper isolation for attachments. */
class MediaAttachmentTest {

    @TempDir
    Path tempDir;

    private Memory entry;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.useDatabaseFile(tempDir.resolve("media.db"));
        MediaStore.useRoot(tempDir.resolve("vault"));
        AuthService.register("curator", "secret123");
        entry = JournalManager.createEntry("Gallery", "See attached.", null, List.of(), null);
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
        MediaStore.resetRoot();
    }

    private Path sourceFile(String name) throws Exception {
        Path file = tempDir.resolve(name);
        Files.write(file, new byte[]{1, 2, 3, 4, 5});
        return file;
    }

    @Test
    void attachListRemoveRoundTrip() throws Exception {
        Media photo = JournalManager.attachMedia(entry.getId(), sourceFile("pic.jpg"));
        assertEquals(MediaKind.PHOTO, photo.kind());
        assertTrue(Files.isRegularFile(photo.getFilePath()), "Vault must hold a copy");
        assertFalse(photo.getFilePath().equals(tempDir.resolve("pic.jpg")), "Originals stay outside");

        Media audio = JournalManager.attachMedia(entry.getId(), sourceFile("note.mp3"));
        List<Media> all = JournalManager.listMedia(entry.getId());
        assertEquals(2, all.size());
        assertEquals(MediaKind.AUDIO, all.get(1).kind());

        Path storedAudio = audio.getFilePath();
        JournalManager.removeMedia(audio.getId());
        assertEquals(1, JournalManager.listMedia(entry.getId()).size());
        assertFalse(Files.exists(storedAudio), "Vault file goes with its row");
    }

    @Test
    void clipAttachRoundTrip() throws Exception {
        Media clip = JournalManager.attachMedia(entry.getId(), sourceFile("spell.mp4"));
        assertEquals(MediaKind.VIDEO, clip.kind());
        assertTrue(Files.isRegularFile(clip.getFilePath()), "Vault must hold a copy");
        assertEquals(1, JournalManager.listMedia(entry.getId()).size());
    }

    @Test
    void unsupportedFilesRejectedCleanly() throws Exception {        Path bad = sourceFile("notes.txt");
        assertThrows(MediaException.class, () -> JournalManager.attachMedia(entry.getId(), bad));
        assertTrue(JournalManager.listMedia(entry.getId()).isEmpty());
    }

    @Test
    void strangersCannotTouchAttachments() throws Exception {
        JournalManager.attachMedia(entry.getId(), sourceFile("mine.png"));
        AuthService.logout();
        AuthService.register("prowler", "otherpass1");

        assertThrows(JournalException.class,
                () -> JournalManager.attachMedia(entry.getId(), sourceFile("x.jpg")));
        assertThrows(JournalException.class, () -> JournalManager.listMedia(entry.getId()));
    }
}
