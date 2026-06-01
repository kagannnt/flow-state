package com.flowstate.timer;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Countdown timer that emits one tick per second on a dedicated worker thread.
 */
public final class TimerEngine implements AutoCloseable {

    private final int totalSeconds;
    private final TimerCallback callback;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final CountDownLatch completionLatch = new CountDownLatch(1);

    private Thread workerThread;

    public TimerEngine(int totalSeconds, TimerCallback callback) {
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("totalSeconds must be positive");
        }
        this.totalSeconds = totalSeconds;
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Timer is already running");
        }

        workerThread = new Thread(this::runCountdown, "flow-state-timer");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void cancel() {
        cancelled.set(true);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public void awaitCompletion() throws InterruptedException {
        completionLatch.await();
    }

    public boolean wasCancelled() {
        return cancelled.get();
    }

    public int totalSeconds() {
        return totalSeconds;
    }

    @Override
    public void close() {
        cancel();
    }

    private void runCountdown() {
        try {
            for (int remaining = totalSeconds; remaining >= 0; remaining--) {
                if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }

                callback.onTick(remaining, totalSeconds);

                if (remaining == 0) {
                    break;
                }

                Thread.sleep(1000L);
            }

            if (!cancelled.get()) {
                callback.onComplete();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            running.set(false);
            completionLatch.countDown();
        }
    }
}
