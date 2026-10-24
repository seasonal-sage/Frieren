package com.timecapsule.media;

import java.nio.file.Path;
import java.time.Instant;

/** A small moving picture tucked between the pages (plays in its own window). */
public class Clip extends Media {

    public Clip(long id, long memoryId, Path filePath, Instant createdAt) {
        super(id, memoryId, filePath, createdAt);
    }

    @Override
    public MediaKind kind() {
        return MediaKind.VIDEO;
    }
}
