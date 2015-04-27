package com.nbarraille.loom;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.SuccessEvent;

public class TaskStatus {
    public static final int PENDING = 0;
    public static final int STARTED = 1;
    public static final int FINISHED = 2;
    public static final int CANCELLED = 3;

    private int mStatus;
    private SuccessEvent mSuccessEvent;
    private FailureEvent mFailureEvent;

    public TaskStatus() {
        mStatus = PENDING;
    }

    synchronized void setStarted() {
        mStatus = STARTED;
    }

    synchronized void setSuccess(SuccessEvent event) {
        mSuccessEvent = event;
        mStatus = FINISHED;
    }

    synchronized void setFailure(FailureEvent event) {
        mFailureEvent = event;
        mStatus = FINISHED;
    }

    synchronized void setCancelled() {
        mStatus = CANCELLED;
    }

    public synchronized boolean isPending() {
        return mStatus == PENDING;
    }

    public synchronized int getStatus() {
        return mStatus;
    }

    public synchronized boolean isStarted() {
        return mStatus == STARTED;
    }

    public synchronized boolean isFinished() {
        return mStatus == FINISHED;
    }

    public synchronized boolean isCancelled() { return mStatus == CANCELLED; }

    public synchronized SuccessEvent getSuccessEvent() {
        return mSuccessEvent;
    }

    public synchronized FailureEvent getFailureEvent() {
        return mFailureEvent;
    }
}
