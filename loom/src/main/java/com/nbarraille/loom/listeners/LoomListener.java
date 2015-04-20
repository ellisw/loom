package com.nbarraille.loom.listeners;

import android.support.annotation.NonNull;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener that contains a set of callbacks being called at various points during the task
 * execution.
 * A Listener needs to be registered with the TaskManager for its callbacks to be called.
 */
public interface LoomListener<Success, Failure, Progress> {
    /**
     * Callback getting executed when the task finishes successfully
     * @param event the success event
     */
    void onSuccess(Success event);

    /**
     * Callback getting executed when the task fails
     * @param event the failure event
     */
    void onFailure(Failure event);

    /**
     * Callback getting executed when the progress of the task changes
     * Fly does not send any progress by default, it is up to individual task to report their
     * progress change
     * @param event the progress event
     */
    void onProgress(Progress event);

    /**
     * The name of the task we're monitoring
     * @return the name of the task
     */
    @NonNull String taskName();
}
