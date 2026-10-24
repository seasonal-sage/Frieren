package com.timecapsule.media;

/** Kinds of attachments a memory can hold. */
public enum MediaKind {

    PHOTO("Photo"),
    AUDIO("Audio"),
    VIDEO("Moving picture");

    private final String label;

    MediaKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Detects the kind from a file extension (case-insensitive). */
    public static MediaKind fromExtension(String extension) {
        String ext = extension == null ? "" : extension.strip().toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "webp":
            case "bmp":
                return PHOTO;
            case "mp3":
            case "wav":
            case "m4a":
            case "ogg":
            case "flac":
                return AUDIO;
            case "mp4":
            case "m4v":
            case "mov":
                return VIDEO;
            default:
                throw new MediaException("That file's magic is unknown to the book (." + ext + ").");
        }
    }
}
