package com.flowstate.tracking;

import com.flowstate.config.SessionMode;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of a completed focus session.
 */
public final class SessionRecord {

    private final Instant completedAt;
    private final SessionMode mode;
    private final int durationSeconds;
    private final boolean completedFully;

    public SessionRecord(
            Instant completedAt,
            SessionMode mode,
            int durationSeconds,
            boolean completedFully) {
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
        this.mode = Objects.requireNonNull(mode, "mode");
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }
        this.durationSeconds = durationSeconds;
        this.completedFully = completedFully;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public SessionMode mode() {
        return mode;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public boolean completedFully() {
        return completedFully;
    }
}
