package com.timecapsule.media;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Builds the right {@link Media} subtype (Factory Pattern). Callers never
 * touch {@link Photo}, {@link Audio}, or {@link Clip} constructors for new attachments.
 */
public final class MediaFactory {

    private MediaFactory() {
    }

    /** Creates an unsaved attachment (id 0) from a vault file. */
    public static Media create(long memoryId, Path vaultFile) {
        if (vaultFile == null) {
            throw new MediaException("No file was offered to the book.");
        }
        MediaKind kind = MediaKind.fromExtension(extensionOf(vaultFile.getFileName().toString()));
        return build(0, memoryId, kind, vaultFile, null);
    }

    /** Rebuilds a persisted row. */
    public static Media fromRow(long id, long memoryId, String type, Path filePath, Instant createdAt) {
        MediaKind kind;
        try {
            kind = MediaKind.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new MediaException("Unknown attachment kind in the archive: " + type);
        }
        return build(id, memoryId, kind, filePath, createdAt);
    }

    private static Media build(long id, long memoryId, MediaKind kind, Path file, Instant createdAt) {
        switch (kind) {
            case PHOTO:
                return new Photo(id, memoryId, file, createdAt);
            case AUDIO:
                return new Audio(id, memoryId, file, createdAt);
            case VIDEO:
                return new Clip(id, memoryId, file, createdAt);
            default:
                throw new MediaException("Unsupported attachment kind: " + kind);
        }
    }

    static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new MediaException("That file wears no extension the book recognises.");
        }
        return fileName.substring(dot + 1);
    }
}
