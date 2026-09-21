package com.timecapsule.media;

/** Friendly failure for unsupported or unreadable attachments. */
public class MediaException extends RuntimeException {

    public MediaException(String message) {
        super(message);
    }

    public MediaException(String message, Throwable cause) {
        super(message, cause);
    }
}
