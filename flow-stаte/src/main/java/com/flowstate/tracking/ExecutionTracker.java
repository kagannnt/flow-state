package com.flowstate.tracking;

import com.flowstate.config.SessionConfiguration;
import com.flowstate.config.SessionMode;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Tracks elapsed time and completion state for the active session.
 */
public final class ExecutionTracker {

    private final SessionConfiguration configuration;
    private final Instant startedAt;
    private Instant endedAt;
    private boolean completedFully;

    public ExecutionTracker(SessionConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.startedAt = Instant.now();
    }

    public SessionConfiguration configuration() {
        return configuration;
    }

    public SessionMode mode() {
        return configuration.mode();
    }

    public int plannedDurationSeconds() {
        return configuration.durationSeconds();
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> endedAt() {
        return Optional.ofNullable(endedAt);
    }

    public void markCompleted() {
        this.completedFully = true;
        this.endedAt = Instant.now();
    }

    public void markInterrupted() {
        this.completedFully = false;
        this.endedAt = Instant.now();
    }

    public int elapsedSeconds() {
        Instant end = endedAt != null ? endedAt : Instant.now();
        long seconds = Duration.between(startedAt, end).getSeconds();
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    public SessionRecord toRecord() {
        Instant completionTime = endedAt != null ? endedAt : Instant.now();
        int duration = completedFully
                ? configuration.durationSeconds()
                : elapsedSeconds();

        return new SessionRecord(
                completionTime,
                configuration.mode(),
                duration,
                completedFully);
    }
}
