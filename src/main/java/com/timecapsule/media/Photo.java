package com.timecapsule.media;

import java.nio.file.Path;
import java.time.Instant;

/** A still image pressed between the pages. */
public class Photo extends Media {

    public Photo(long id, long memoryId, Path filePath, Instant createdAt) {
        super(id, memoryId, filePath, createdAt);
    }

    @Override
    public MediaKind kind() {
        return MediaKind.PHOTO;
    }
}
