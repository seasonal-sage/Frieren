package com.timecapsule.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * The vault: attachments are copied under {@code ~/.timecapsule/media/}
 * with random names, so renames/deletes outside never break the archive.
 */
public final class MediaStore {

    private static Path rootOverride;

    private MediaStore() {
    }

    /** Copies a file into the keeper's vault, returning its stored path. */
    public static Path copyToVault(long userId, Path source) {
        if (source == null || !Files.isRegularFile(source)) {
            throw new MediaException("That file does not exist.");
        }
        // Validate kind BEFORE copying so rejects leave no orphans.
        String ext = MediaFactory.extensionOf(source.getFileName().toString());
        MediaKind.fromExtension(ext);
        try {
            Path dir = root().resolve(String.valueOf(userId));
            Files.createDirectories(dir);
            Path dest = dir.resolve(UUID.randomUUID() + "." + ext.toLowerCase());
            Files.copy(source, dest);
            return dest;
        } catch (IOException e) {
            throw new MediaException("The vault would not take that file.", e);
        }
    }

    /** Best-effort removal of a vault file (archive row is deleted anyway). */
    public static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Row deletion matters; a stray file is just dust.
        }
    }

    static Path root() {
        if (rootOverride != null) {
            return rootOverride;
        }
        return Path.of(System.getProperty("user.home"), ".timecapsule", "media");
    }

    /** Test hook: redirect the vault to a scratch directory. */
    static void useRoot(Path root) {
        rootOverride = root;
    }

    /** Test hook: restore the default vault location. */
    static void resetRoot() {
        rootOverride = null;
    }
}
