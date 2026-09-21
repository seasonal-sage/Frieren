package com.timecapsule.auth;

import java.time.Instant;

/**
 * A keeper of the grimoire (Module: Auth & Identity). Plain data holder —
 * all behaviour lives in {@link AuthService} (single responsibility).
 */
public class User {

    private final long id;
    private final String username;
    private final String passwordHash;
    private final byte[] encKeySalt;
    private final Instant createdAt;

    public User(long id, String username, String passwordHash, byte[] encKeySalt, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.encKeySalt = encKeySalt == null ? null : encKeySalt.clone();
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public byte[] getEncKeySalt() {
        return encKeySalt == null ? null : encKeySalt.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
