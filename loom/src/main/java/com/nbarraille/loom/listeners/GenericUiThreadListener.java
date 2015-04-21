package com.nbarraille.loom.listeners;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener for GreenRobot's EventBus that receives callbacks on the UI Thread
 */
public abstract class GenericUiThreadListener implements LoomListener<SuccessEvent, FailureEvent, ProgressEvent> {
    @SuppressWarnings("unused")
    public final void onEventMainThread(SuccessEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onSuccess(event);
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(FailureEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onFailure(event);
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(ProgressEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onProgress(event);
        }
    }

    @Override
    public void onSuccess(SuccessEvent event) {}

    @Override
    public void onFailure(FailureEvent event) {}

    @Override
    public void onProgress(ProgressEvent event) {}
}
