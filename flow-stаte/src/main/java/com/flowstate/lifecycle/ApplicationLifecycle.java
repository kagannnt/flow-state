package com.flowstate.lifecycle;

import com.flowstate.cli.CliArguments;
import com.flowstate.cli.CliParser;
import com.flowstate.config.SessionConfiguration;
import com.flowstate.config.SessionMode;
import com.flowstate.logging.HistoryLogger;
import com.flowstate.timer.TimerCallback;
import com.flowstate.timer.TimerEngine;
import com.flowstate.tracking.ExecutionTracker;
import com.flowstate.tracking.SessionRecord;
import com.flowstate.ui.TerminalRenderer;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates CLI flows, the timer engine, tracking, rendering, and history persistence.
 */
public final class ApplicationLifecycle {

    private final CliParser cliParser = new CliParser();

    public int run(CliArguments arguments) {
        try (HistoryLogger historyLogger = new HistoryLogger(arguments.historyFile())) {
            if (arguments.listModes()) {
                System.out.println(cliParser.formatModes());
                return 0;
            }

            if (arguments.showHistory()) {
                System.out.println(historyLogger.formatHistory());
                return 0;
            }

            SessionMode mode = arguments.selectedMode()
                    .orElseGet(this::promptForMode);

            return runSession(new SessionConfiguration(mode), historyLogger);
        }
    }

    private int runSession(SessionConfiguration configuration, HistoryLogger historyLogger) {
        ExecutionTracker tracker = new ExecutionTracker(configuration);
        TerminalRenderer renderer = new TerminalRenderer(configuration.mode());
        renderer.printBanner();

        AtomicReference<TimerEngine> engineReference = new AtomicReference<>();

        Thread shutdownHook = new Thread(() -> handleShutdown(engineReference.get(), tracker, renderer, historyLogger));
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        TimerCallback callback = new TimerCallback() {
            @Override
            public void onTick(int remainingSeconds, int totalSeconds) {
                renderer.onTick(remainingSeconds, totalSeconds);
            }

            @Override
            public void onComplete() {
                tracker.markCompleted();
            }
        };

        TimerEngine engine = new TimerEngine(configuration.durationSeconds(), callback);
        engineReference.set(engine);

        try {
            engine.start();
            engine.awaitCompletion();

            if (engine.wasCancelled()) {
                return 130;
            }

            if (tracker.endedAt().isEmpty()) {
                tracker.markCompleted();
            }

            SessionRecord record = tracker.toRecord();
            historyLogger.appendAsync(record);
            historyLogger.close();

            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            engine.cancel();
            tracker.markInterrupted();
            renderer.printInterrupted();
            historyLogger.appendAsync(tracker.toRecord());
            return 130;
        } finally {
            removeShutdownHook(shutdownHook);
            engine.close();
        }
    }

    private void handleShutdown(
            TimerEngine engine,
            ExecutionTracker tracker,
            TerminalRenderer renderer,
            HistoryLogger historyLogger) {
        if (engine == null || tracker.endedAt().isPresent()) {
            return;
        }

        engine.cancel();
        tracker.markInterrupted();
        renderer.printInterrupted();
        historyLogger.appendAsync(tracker.toRecord());
    }

    private void removeShutdownHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down
        }
    }

    private SessionMode promptForMode() {
        System.out.println("Select a session mode:");
        SessionMode[] modes = SessionMode.values();
        for (int index = 0; index < modes.length; index++) {
            SessionMode mode = modes[index];
            System.out.printf("  %d) %s (%d min)%n",
                    index + 1,
                    mode.displayName(),
                    mode.durationSeconds() / 60);
        }
        System.out.print("Choice [1]: ");

        try (Scanner scanner = new Scanner(System.in)) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return modes[0];
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= modes.length) {
                    return modes[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                return SessionMode.fromToken(input).orElse(modes[0]);
            }
        }

        return modes[0];
    }
}
