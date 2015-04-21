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

    @Override
    public boolean equals(Object o) {
        if (o instanceof ProgressEvent) {
            return mProgress == ((ProgressEvent) o).getProgress();
        } else {
            return false;
        }
    }
}
