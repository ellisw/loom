package com.nbarraille.loom.events;

/**
 * The base class for a Progress event
 */
public abstract class ProgressEvent {
    private final int mProgress;

    public ProgressEvent(int progress) {
        mProgress = progress;
    }

    @SuppressWarnings("unused")
    public int getProgress() {
        return mProgress;
    }
}
