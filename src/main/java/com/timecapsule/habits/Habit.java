package com.timecapsule.habits;

import java.time.Instant;

/**
 * A recurring practice the keeper tracks (Module: Analytics & Insights,
 * Phase 2). Plain data — behaviour lives in {@link HabitService}.
 */
public class Habit {

    private final long id;
    private final long userId;
    private final String name;
    private final int targetPerWeek;
    private final Instant createdAt;

    public Habit(long id, long userId, String name, int targetPerWeek, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetPerWeek = targetPerWeek;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getTargetPerWeek() {
        return targetPerWeek;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
