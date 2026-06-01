package com.flowstate.logging;

import com.flowstate.tracking.SessionRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persists session records asynchronously so the timer thread never blocks on I/O.
 */
public final class HistoryLogger implements AutoCloseable {

    private final Path historyFile;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public HistoryLogger(Path historyFile) {
        this.historyFile = Objects.requireNonNull(historyFile, "historyFile");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "flow-state-history");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void appendAsync(SessionRecord record) {
        Objects.requireNonNull(record, "record");
        if (closed.get()) {
            return;
        }

        executor.submit(() -> persistRecord(record));
    }

    public List<SessionRecord> readAll() throws IOException {
        if (!Files.exists(historyFile)) {
            return List.of();
        }

        String content = Files.readString(historyFile, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return List.of();
        }

        return HistoryJsonCodec.decodeArray(content);
    }

    public String formatHistory() {
        try {
            List<SessionRecord> records = readAll();
            if (records.isEmpty()) {
                return "No completed sessions recorded yet.";
            }

            StringBuilder builder = new StringBuilder("Session history (")
                    .append(historyFile.toAbsolutePath())
                    .append("):\n");

            for (SessionRecord record : records) {
                builder.append(String.format(
                        "  %s  %-20s  %2dm %02ds  %s%n",
                        record.completedAt(),
                        record.mode().displayName(),
                        record.durationSeconds() / 60,
                        record.durationSeconds() % 60,
                        record.completedFully() ? "completed" : "interrupted"));
            }
            return builder.toString().stripTrailing();
        } catch (IOException exception) {
            return "Unable to read history: " + exception.getMessage();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void persistRecord(SessionRecord record) {
        try {
            List<SessionRecord> existing = new ArrayList<>();
            if (Files.exists(historyFile)) {
                String raw = Files.readString(historyFile, StandardCharsets.UTF_8).trim();
                if (!raw.isEmpty()) {
                    existing.addAll(HistoryJsonCodec.decodeArray(raw));
                }
            }

            List<SessionRecord> updated = new ArrayList<>(existing);
            updated.add(record);

            Path parent = historyFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String json = HistoryJsonCodec.encodeArray(updated);
            Files.writeString(
                    historyFile,
                    json + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            System.err.println("Warning: failed to write session history — " + exception.getMessage());
        }
    }
}
