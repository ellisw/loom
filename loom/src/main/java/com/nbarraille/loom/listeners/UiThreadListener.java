package com.nbarraille.loom.listeners;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener for GreenRobot's EventBus that receives callbacks on the UI Thread
 */
public abstract class UiThreadListener<Success extends SuccessEvent, Failure extends FailureEvent, Progress extends ProgressEvent>
        implements LoomListener<Success, Failure, Progress> {
    @SuppressWarnings("unused")
    public final void onEventMainThread(SuccessEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onSuccess((Success) event);
            } catch (ClassCastException e) {}
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(FailureEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onFailure((Failure) event);
            } catch (ClassCastException e) {}
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(ProgressEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onProgress((Progress) event);
            } catch (ClassCastException e) {}
        }
    }

    @Override
    public void onSuccess(Success event) {}

    @Override
    public void onFailure(Failure event) {}

    @Override
    public void onProgress(Progress event) {}
}
