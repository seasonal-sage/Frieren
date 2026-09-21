package com.timecapsule.core;

import com.timecapsule.audit.AuditLogger;
import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import com.timecapsule.media.Media;
import com.timecapsule.media.MediaException;
import com.timecapsule.media.MediaFactory;
import com.timecapsule.media.MediaStore;
import com.timecapsule.security.CryptoService;
import com.timecapsule.storage.MediaDao;
import com.timecapsule.storage.MediaDao.MediaRow;
import com.timecapsule.storage.MemoryDao;
import com.timecapsule.storage.MemoryDao.MemoryRow;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entry operations (Module: Memory Core Engine). Owns the lock contract:
 * a sealed entry's body is never decrypted before its date, no matter which
 * UI screen asks. Holds no state — the session comes from {@link AuthService}.
 */
public final class JournalManager {

    private static final MemoryDao MEMORIES = new MemoryDao();
    private static final MediaDao MEDIA = new MediaDao();

    private JournalManager() {
    }

    /** Creates an entry; a future {@code unlockDate} seals it as a capsule. */
    public static Memory createEntry(String title, String body, Mood mood,
                                     List<String> tags, Instant unlockDate) {
        User user = requireUser();
        String cleanTitle = validateTitle(title);
        if (body == null) {
            throw new JournalException("Entry body cannot be null.");
        }
        Instant now = Instant.now();
        try {
            CryptoService.Sealed sealed = CryptoService.encryptText(body, AuthService.sessionKey());
            long id = MEMORIES.insert(user.getId(), cleanTitle, sealed.cipher(), sealed.iv(),
                    mood == null ? null : mood.name(), toCsv(tags),
                    unlockDate != null, unlockDate);
            AuditLogger.log(user.getId(), "entry.create " + id);
            return getEntry(id);
        } catch (JournalException e) {
            throw e;
        } catch (Exception e) {
            throw new JournalException("The ink would not dry — entry not saved.", e);
        }
    }

    /** Newest-first entries; sealed ones arrive with a {@code null} body. */
    public static List<Memory> listEntries() {
        User user = requireUser();
        Instant now = Instant.now();
        try {
            return MEMORIES.listByUser(user.getId()).stream()
                    .map(row -> toMemory(row, now))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new JournalException("Could not open the pages.", e);
        }
    }

    public static Memory getEntry(long id) {
        User user = requireUser();
        try {
            MemoryRow row = MEMORIES.findById(id, user.getId())
                    .orElseThrow(() -> new JournalException("No entry answers to that number."));
            return toMemory(row, Instant.now());
        } catch (JournalException e) {
            throw e;
        } catch (SQLException e) {
            throw new JournalException("Could not open that entry.", e);
        }
    }

    public static Memory updateEntry(long id, String title, String body, Mood mood,
                                     List<String> tags, Instant unlockDate) {
        User user = requireUser();
        String cleanTitle = validateTitle(title);
        if (body == null) {
            throw new JournalException("Entry body cannot be null.");
        }
        // Ownership check first — strangers may not rewrite pages.
        getEntry(id);
        try {
            CryptoService.Sealed sealed = CryptoService.encryptText(body, AuthService.sessionKey());
            MEMORIES.update(id, user.getId(), cleanTitle, sealed.cipher(), sealed.iv(),
                    mood == null ? null : mood.name(), toCsv(tags),
                    unlockDate != null, unlockDate);
            AuditLogger.log(user.getId(), "entry.update " + id);
            return getEntry(id);
        } catch (JournalException e) {
            throw e;
        } catch (Exception e) {
            throw new JournalException("The rewrite would not take.", e);
        }
    }

    public static void deleteEntry(long id) {
        User user = requireUser();
        try {
            if (!MEMORIES.delete(id, user.getId())) {
                throw new JournalException("No entry answers to that number.");
            }
            AuditLogger.log(user.getId(), "entry.delete " + id);
        } catch (JournalException e) {
            throw e;
        } catch (SQLException e) {
            throw new JournalException("That page would not tear out.", e);
        }
    }

    /** Sealed entries whose date has come (feeds the Day-5 scheduler). */
    public static List<Memory> listDueEntries() {
        User user = requireUser();
        Instant now = Instant.now();
        try {
            return MEMORIES.listDue(user.getId(), now).stream()
                    .map(row -> toMemory(row, now))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new JournalException("Could not check the seals.", e);
        }
    }

    /**
     * Presses a photo or voice clip into an entry. The file is copied into
     * the keeper's vault first, so later renames outside change nothing.
     */
    public static Media attachMedia(long entryId, Path sourceFile) {
        User user = requireUser();
        getEntry(entryId); // Ownership check — strangers attach nothing.
        try {
            Path stored = MediaStore.copyToVault(user.getId(), sourceFile);
            Media media = MediaFactory.create(entryId, stored);
            long id = MEDIA.insert(entryId, media.kind().name(), stored.toString());
            AuditLogger.log(user.getId(), "media.attach " + id);
            return MediaFactory.fromRow(id, entryId, media.kind().name(), stored, null);
        } catch (JournalException | MediaException e) {
            throw e;
        } catch (Exception e) {
            throw new JournalException("That attachment would not stick.", e);
        }
    }

    /** Attachments of an entry, oldest first. */
    public static List<Media> listMedia(long entryId) {
        User user = requireUser();
        getEntry(entryId); // Ownership check.
        try {
            return MEDIA.listByMemory(entryId, user.getId()).stream()
                    .map(MediaDao::toMedia)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new JournalException("Could not gather the attachments.", e);
        }
    }

    /** Tears an attachment out (archive row plus vault file). */
    public static void removeMedia(long mediaId) {
        User user = requireUser();
        try {
            MediaRow row = MEDIA.findById(mediaId, user.getId())
                    .orElseThrow(() -> new JournalException("No attachment answers to that number."));
            if (!MEDIA.delete(mediaId, user.getId())) {
                throw new JournalException("No attachment answers to that number.");
            }
            MediaStore.deleteQuietly(Path.of(row.filePath()));
            AuditLogger.log(user.getId(), "media.remove " + mediaId);
        } catch (JournalException e) {
            throw e;
        } catch (SQLException e) {
            throw new JournalException("That attachment would not tear out.", e);
        }
    }

    private static Memory toMemory(MemoryRow row, Instant now) {
        boolean sealed = row.locked() && row.unlockDate() != null && row.unlockDate().isAfter(now);
        String body = null;
        if (!sealed) {
            try {
                body = CryptoService.decryptText(
                        new CryptoService.Sealed(row.iv(), row.cipher()), AuthService.sessionKey());
            } catch (Exception e) {
                throw new JournalException("That entry's seal is broken (decryption failed).", e);
            }
        }
        return new Memory(row.id(), row.userId(), row.title(), body, Mood.parse(row.mood()),
                fromCsv(row.tags()), row.locked(), row.unlockDate(),
                row.createdAt(), row.updatedAt());
    }

    private static User requireUser() {
        User user = AuthService.currentUser();
        if (user == null) {
            throw new JournalException("No keeper is signed in.");
        }
        return user;
    }

    private static String validateTitle(String title) {
        String clean = title == null ? "" : title.strip();
        if (clean.isEmpty()) {
            throw new JournalException("Entries need a title.");
        }
        if (clean.length() > 200) {
            throw new JournalException("Titles run at most 200 letters.");
        }
        return clean;
    }

    static String toCsv(List<String> tags) {
        if (tags == null) {
            return "";
        }
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::strip)
                .collect(Collectors.joining(","));
    }

    static List<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }
}
