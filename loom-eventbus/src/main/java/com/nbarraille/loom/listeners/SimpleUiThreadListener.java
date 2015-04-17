package com.nbarraille.loom.listeners;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A simple implementation of UiThreadListener that doesn't do anything, to be used if you don't
 * want to override all the callbacks
 */
public abstract class SimpleUiThreadListener<Success extends SuccessEvent, Failure extends FailureEvent, Progress extends ProgressEvent>
        extends UiThreadListener<Success, Failure, Progress> {
    @Override
    public void onSuccess(Success event) {}

    @Override
    public void onFailure(Failure event) {}

    @Override
    public void onProgress(Progress event) {}
}
