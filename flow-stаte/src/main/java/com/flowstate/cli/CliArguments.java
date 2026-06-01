package com.flowstate.cli;

import com.flowstate.config.SessionMode;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Parsed command-line options.
 */
public final class CliArguments {

    private final boolean showHelp;
    private final boolean listModes;
    private final boolean showHistory;
    private final Optional<SessionMode> selectedMode;
    private final Path historyFile;

    public CliArguments(
            boolean showHelp,
            boolean listModes,
            boolean showHistory,
            Optional<SessionMode> selectedMode,
            Path historyFile) {
        this.showHelp = showHelp;
        this.listModes = listModes;
        this.showHistory = showHistory;
        this.selectedMode = selectedMode;
        this.historyFile = historyFile;
    }

    public boolean showHelp() {
        return showHelp;
    }

    public boolean listModes() {
        return listModes;
    }

    public boolean showHistory() {
        return showHistory;
    }

    public Optional<SessionMode> selectedMode() {
        return selectedMode;
    }

    public Path historyFile() {
        return historyFile;
    }
}
