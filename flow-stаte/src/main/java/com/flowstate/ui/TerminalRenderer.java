package com.flowstate.ui;

import com.flowstate.config.SessionMode;
import com.flowstate.timer.TimerCallback;

/**
 * Renders a distraction-free countdown and progress bar in the terminal.
 */
public final class TerminalRenderer implements TimerCallback {

    private static final int BAR_WIDTH = 40;

    private final SessionMode mode;
    private final Object renderLock = new Object();
    private boolean headerPrinted;

    public TerminalRenderer(SessionMode mode) {
        this.mode = mode;
    }

    public void printBanner() {
        synchronized (renderLock) {
            System.out.println();
            System.out.println("flow-state");
            System.out.println("  " + mode.displayName() + " · press Ctrl+C to stop");
            System.out.println();
            headerPrinted = true;
        }
    }

    @Override
    public void onTick(int remainingSeconds, int totalSeconds) {
        synchronized (renderLock) {
            if (!headerPrinted) {
                printBanner();
            }

            double progress = totalSeconds == 0
                    ? 1.0
                    : (double) (totalSeconds - remainingSeconds) / totalSeconds;

            String bar = buildProgressBar(progress);
            String clock = formatClock(remainingSeconds);

            System.out.print("\r  " + bar + "  " + clock + "   ");
            System.out.flush();
        }
    }

    @Override
    public void onComplete() {
        synchronized (renderLock) {
            onTick(0, 1);
            System.out.println();
            System.out.println();
            System.out.println("  Session complete. Well done.");
            System.out.println();
        }
    }

    public void printInterrupted() {
        synchronized (renderLock) {
            System.out.println();
            System.out.println();
            System.out.println("  Session interrupted.");
            System.out.println();
        }
    }

    private String buildProgressBar(double progress) {
        int clampedFilled = (int) Math.round(Math.clamp(progress, 0.0, 1.0) * BAR_WIDTH);
        StringBuilder bar = new StringBuilder(BAR_WIDTH + 2);
        bar.append('[');
        for (int index = 0; index < BAR_WIDTH; index++) {
            bar.append(index < clampedFilled ? '█' : '░');
        }
        bar.append(']');
        return bar.toString();
    }

    private String formatClock(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
