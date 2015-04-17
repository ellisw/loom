package com.nbarraille.loom.listeners;

import com.nbarraille.loom.LoomListener;
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
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onSuccess((Success) event);
        } catch(ClassCastException e) {}
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(FailureEvent event) {
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onFailure((Failure) event);
        } catch(ClassCastException e) {}
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(ProgressEvent event) {
        //noinspection EmptyCatchBlock
        try {
            //noinspection unchecked
            onProgress((Progress) event);
        } catch(ClassCastException e) {}
    }
}
