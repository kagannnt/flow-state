package com.flowstate.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Predefined focus configurations for the timer.
 */
public enum SessionMode {

    DEEP_WORK("Deep Work", 90 * 60),
    TECHNICAL_SESSION("Technical Session", 60 * 60),
    REST_INTERVAL("Rest Interval", 15 * 60);

    private final String displayName;
    private final int durationSeconds;

    SessionMode(String displayName, int durationSeconds) {
        this.displayName = displayName;
        this.durationSeconds = durationSeconds;
    }

    public String displayName() {
        return displayName;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public static Optional<SessionMode> fromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String normalized = token.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return Arrays.stream(values())
                .filter(mode -> mode.name().equals(normalized)
                        || mode.displayName.toUpperCase(Locale.ROOT).replace(' ', '_').equals(normalized))
                .findFirst();
    }
}
