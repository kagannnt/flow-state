package com.flowstate.timer;

/**
 * Observes timer progress from the engine thread.
 */
public interface TimerCallback {

    void onTick(int remainingSeconds, int totalSeconds);

    void onComplete();
}
