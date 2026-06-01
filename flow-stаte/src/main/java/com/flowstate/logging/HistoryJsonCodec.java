package com.flowstate.logging;

import com.flowstate.config.SessionMode;
import com.flowstate.tracking.SessionRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON encoder/decoder for session history without external dependencies.
 */
final class HistoryJsonCodec {

    private static final Pattern OBJECT_PATTERN = Pattern.compile(
            "\\{\\s*\"completedAt\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"mode\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*"
                    + "\"durationSeconds\"\\s*:\\s*(\\d+)\\s*,\\s*"
                    + "\"completedFully\"\\s*:\\s*(true|false)\\s*\\}",
            Pattern.DOTALL);

    private HistoryJsonCodec() {
    }

    static String encodeArray(List<SessionRecord> records) {
        StringBuilder builder = new StringBuilder("[\n");
        for (int index = 0; index < records.size(); index++) {
            if (index > 0) {
                builder.append(",\n");
            }
            builder.append(encodeObject(records.get(index)));
        }
        builder.append("\n]");
        return builder.toString();
    }

    static List<SessionRecord> decodeArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("History file must contain a JSON array");
        }

        List<SessionRecord> records = new ArrayList<>();
        Matcher matcher = OBJECT_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            Instant completedAt = Instant.parse(matcher.group(1));
            SessionMode mode = SessionMode.valueOf(matcher.group(2));
            int durationSeconds = Integer.parseInt(matcher.group(3));
            boolean completedFully = Boolean.parseBoolean(matcher.group(4));
            records.add(new SessionRecord(completedAt, mode, durationSeconds, completedFully));
        }
        return records;
    }

    private static String encodeObject(SessionRecord record) {
        return "  {"
                + "\"completedAt\":\"" + record.completedAt() + "\","
                + "\"mode\":\"" + record.mode().name() + "\","
                + "\"durationSeconds\":" + record.durationSeconds() + ","
                + "\"completedFully\":" + record.completedFully()
                + "}";
    }
}
