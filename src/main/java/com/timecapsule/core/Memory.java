package com.timecapsule.core;

import java.time.Instant;
import java.util.List;

/**
 * One diary entry (Module: Memory Core Engine). Pure data — all behaviour
 * lives in {@link JournalManager}. The body is {@code null} while the
 * time-capsule seal holds; the manager never decrypts early.
 */
public class Memory {

    private final long id;
    private final long userId;
    private final String title;
    private final String body;
    private final Mood mood;
    private final List<String> tags;
    private final boolean locked;
    private final Instant unlockDate;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Memory(long id, long userId, String title, String body, Mood mood,
                  List<String> tags, boolean locked, Instant unlockDate,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.mood = mood;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.locked = locked;
        this.unlockDate = unlockDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** True once the seal has lifted (or was never locked) at the moment. */
    public boolean isUnlockedAt(Instant now) {
        return !locked || unlockDate == null || !unlockDate.isAfter(now);
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    /** Plaintext body, or {@code null} while sealed. */
    public String getBody() {
        return body;
    }

    public Mood getMood() {
        return mood;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isLocked() {
        return locked;
    }

    public Instant getUnlockDate() {
        return unlockDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
