package com.timecapsule.media;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves kind detection and factory polymorphism. */
class MediaFactoryTest {

    @Test
    void photosDetected() {
        for (String ext : new String[]{"jpg", "JPEG", "png", "gif", "webp"}) {
            Media media = MediaFactory.create(1, Path.of("vault/pic." + ext));
            assertEquals(MediaKind.PHOTO, media.kind());
            assertTrue(media instanceof Photo);
        }
    }

    @Test
    void audioDetected() {
        for (String ext : new String[]{"mp3", "WAV", "m4a", "ogg"}) {
            Media media = MediaFactory.create(1, Path.of("vault/note." + ext));
            assertEquals(MediaKind.AUDIO, media.kind());
            assertTrue(media instanceof Audio);
        }
    }

    @Test
    void videosDetected() {
        for (String ext : new String[]{"mp4", "M4V", "mov"}) {
            Media media = MediaFactory.create(1, Path.of("vault/clip." + ext));
            assertEquals(MediaKind.VIDEO, media.kind());
            assertTrue(media instanceof Clip);
        }
    }

    @Test
    void unknownMagicRejected() {        assertThrows(MediaException.class, () -> MediaFactory.create(1, Path.of("vault/doc.txt")));
        assertThrows(MediaException.class, () -> MediaFactory.create(1, Path.of("vault/run.exe")));
        assertThrows(MediaException.class, () -> MediaFactory.create(1, Path.of("vault/noext")));
        assertThrows(MediaException.class, () -> MediaFactory.create(1, null));
    }

    @Test
    void rowsRebuildSubtypes() {
        Media photo = MediaFactory.fromRow(7, 3, "PHOTO", Path.of("v/a.jpg"), null);
        Media audio = MediaFactory.fromRow(8, 3, "AUDIO", Path.of("v/b.mp3"), null);
        Media clip = MediaFactory.fromRow(9, 3, "VIDEO", Path.of("v/c.mp4"), null);
        assertTrue(photo instanceof Photo);
        assertTrue(audio instanceof Audio);
        assertTrue(clip instanceof Clip);
        assertEquals(7, photo.getId());
        assertThrows(MediaException.class,
                () -> MediaFactory.fromRow(9, 3, "SCROLL", Path.of("v/c"), null));
    }
}
