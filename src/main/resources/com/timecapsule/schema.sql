-- Time Capsule schema v1 (Session A).
-- Applied automatically by DatabaseManager on first launch.
-- Conventions: UTC ISO-8601 TEXT timestamps, BOOLEAN as INTEGER 0/1.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    enc_key_salt  BLOB NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS memories (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    encrypted_body BLOB NOT NULL,
    iv             BLOB NOT NULL,
    mood           TEXT,
    tags           TEXT NOT NULL DEFAULT '',
    is_locked      INTEGER NOT NULL DEFAULT 0,
    unlock_date    TEXT,
    created_at     TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at     TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS media (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    memory_id  INTEGER NOT NULL REFERENCES memories (id) ON DELETE CASCADE,
    type       TEXT NOT NULL,
    file_path  TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS habits (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    target_per_week INTEGER NOT NULL DEFAULT 3,
    created_at      TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS habit_logs (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    habit_id  INTEGER NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    log_date  TEXT NOT NULL,
    done      INTEGER NOT NULL DEFAULT 1,
    UNIQUE (habit_id, log_date)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    event      TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       TEXT NOT NULL,
    content    TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE IF NOT EXISTS reminders (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reminder_date TEXT NOT NULL,
    message      TEXT NOT NULL,
    entry_id     INTEGER REFERENCES memories (id) ON DELETE SET NULL,
    repeat_type  TEXT NOT NULL DEFAULT 'none',
    fired        INTEGER NOT NULL DEFAULT 0,
    created_at   TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE INDEX IF NOT EXISTS idx_memories_user_unlock ON memories (user_id, unlock_date);
CREATE INDEX IF NOT EXISTS idx_memories_user_created ON memories (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_media_memory ON media (memory_id);
CREATE INDEX IF NOT EXISTS idx_habit_logs_habit_date ON habit_logs (habit_id, log_date);
CREATE INDEX IF NOT EXISTS idx_chat_user_id ON chat_messages (user_id, id);
CREATE INDEX IF NOT EXISTS idx_reminders_user_date ON reminders (user_id, reminder_date);

CREATE TABLE IF NOT EXISTS checkpoints (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title         TEXT NOT NULL,
    description   TEXT,
    target_date   TEXT,
    status        TEXT NOT NULL DEFAULT 'pending',
    created_at    TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    completed_at  TEXT
);

CREATE INDEX IF NOT EXISTS idx_checkpoints_user_status ON checkpoints (user_id, status);
CREATE INDEX IF NOT EXISTS idx_checkpoints_user_target ON checkpoints (user_id, target_date);
