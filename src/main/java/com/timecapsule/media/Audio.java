package com.timecapsule.media;

import java.nio.file.Path;
import java.time.Instant;

/** A voice clip tucked into the margin. */
public class Audio extends Media {

    public Audio(long id, long memoryId, Path filePath, Instant createdAt) {
        super(id, memoryId, filePath, createdAt);
    }

    @Override
    public MediaKind kind() {
        return MediaKind.AUDIO;
    }
}
