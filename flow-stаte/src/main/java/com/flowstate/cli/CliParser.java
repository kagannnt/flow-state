package com.flowstate.cli;

import com.flowstate.config.SessionMode;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Parses raw CLI arguments into structured options.
 */
public final class CliParser {

    private static final Path DEFAULT_HISTORY = Path.of("history.json");

    public CliArguments parse(String[] args) {
        if (args == null) {
            args = new String[0];
        }

        boolean help = false;
        boolean listModes = false;
        boolean showHistory = false;
        Path historyFile = DEFAULT_HISTORY;
        Optional<SessionMode> mode = Optional.empty();

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];

            switch (arg) {
                case "-h", "--help" -> help = true;
                case "-l", "--list-modes" -> listModes = true;
                case "--history" -> showHistory = true;
                case "--history-file" -> {
                    if (index + 1 >= args.length) {
                        throw new IllegalArgumentException("--history-file requires a path");
                    }
                    historyFile = Path.of(args[++index]);
                }
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    if (mode.isPresent()) {
                        throw new IllegalArgumentException("Only one session mode may be specified");
                    }
                    mode = SessionMode.fromToken(arg);
                    if (mode.isEmpty()) {
                        throw new IllegalArgumentException("Unknown session mode: " + arg);
                    }
                }
            }
        }

        return new CliArguments(help, listModes, showHistory, mode, historyFile);
    }

    public String usage() {
        return """
                flow-state — minimalist focus timer

                Usage:
                  java -jar flow-state.jar <mode>
                  java -jar flow-state.jar [options]

                Modes:
                  deep-work            High-focus block (90 min)
                  technical-session    Structured technical work (60 min)
                  rest-interval        Recovery break (15 min)

                Options:
                  -h, --help           Show this message
                  -l, --list-modes     List available modes and durations
                  --history            Print session history
                  --history-file PATH  History file location (default: history.json)

                Examples:
                  java -jar flow-state.jar deep-work
                  java -jar flow-state.jar --list-modes
                """;
    }

    public String formatModes() {
        StringBuilder builder = new StringBuilder("Available session modes:\n");
        for (SessionMode mode : SessionMode.values()) {
            builder.append(String.format(Locale.ROOT, "  %-20s %s (%d min)%n",
                    mode.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                    mode.displayName(),
                    mode.durationSeconds() / 60));
        }
        return builder.toString().stripTrailing();
    }
}
