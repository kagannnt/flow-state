package com.flowstate.config;

import java.util.Objects;

/**
 * Immutable configuration for a single timed session.
 */
public final class SessionConfiguration {

    private final SessionMode mode;
    private final int durationSeconds;

    public SessionConfiguration(SessionMode mode) {
        this(mode, mode.durationSeconds());
    }

    public SessionConfiguration(SessionMode mode, int durationSeconds) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        this.durationSeconds = durationSeconds;
    }

    public SessionMode mode() {
        return mode;
    }

    public int durationSeconds() {
        return durationSeconds;
    }
}
