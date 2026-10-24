package com.timecapsule.media;

import java.nio.file.Path;
import java.time.Instant;

/**
 * One attachment on a memory (Module: Media Processing). Abstract parent of
 * {@link Photo}, {@link Audio}, and {@link Clip} — inheritance + polymorphism showcase.
 */
public abstract class Media {

    private final long id;
    private final long memoryId;
    private final Path filePath;
    private final Instant createdAt;

    protected Media(long id, long memoryId, Path filePath, Instant createdAt) {
        this.id = id;
        this.memoryId = memoryId;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }

    public abstract MediaKind kind();

    public long getId() {
        return id;
    }

    public long getMemoryId() {
        return memoryId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
