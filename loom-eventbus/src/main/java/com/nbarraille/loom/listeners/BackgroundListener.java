package com.nbarraille.loom.listeners;

import com.nbarraille.loom.LoomListener;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener for GreenRobot's EventBus that receives callbacks on the same background thread
 * where the task was running
 */
public abstract class BackgroundListener<Success extends SuccessEvent, Failure extends FailureEvent, Progress extends ProgressEvent>
        implements LoomListener<Success, Failure, Progress> {
    @SuppressWarnings("unused")
    public final void onEvent(SuccessEvent event) {
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onSuccess((Success) event);
        } catch(ClassCastException e) {}
    }

    @SuppressWarnings("unused")
    public final void onEvent(FailureEvent event) {
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onFailure((Failure) event);
        } catch(ClassCastException e) {}
    }

    @SuppressWarnings("unused")
    public final void onEvent(ProgressEvent event) {
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onProgress((Progress) event);
        } catch(ClassCastException e) {}
    }
}
