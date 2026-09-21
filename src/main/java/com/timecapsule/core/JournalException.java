package com.timecapsule.core;

/** Friendly failure for journal operations (validation, missing entries). */
public class JournalException extends RuntimeException {

    public JournalException(String message) {
        super(message);
    }

    public JournalException(String message, Throwable cause) {
        super(message, cause);
    }
}
