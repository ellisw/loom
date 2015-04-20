package com.nbarraille.loom.events;

/**
 * The base class for a Progress event
 */
public class ProgressEvent extends Event {
    private final int mProgress;

    public ProgressEvent(int progress) {
        mProgress = progress;
    }

    @SuppressWarnings("unused")
    public int getProgress() {
        return mProgress;
    }
}
