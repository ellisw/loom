package com.nbarraille.loom.sample;

import android.support.annotation.Nullable;
import android.util.Log;

import com.nbarraille.loom.Task;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A simple task that reports it's success, failure, and progress to listeners and is cancellable
 */
public class TaskCancellable extends Task<TaskCancellable.Success, TaskCancellable.Failure, TaskCancellable.Progress> {
    public static class Success extends SuccessEvent {}
    public static class Failure extends FailureEvent {}
    public static class Progress extends ProgressEvent {
        public Progress(int progress) {
            super(progress);
        }
    }

    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            Log.i("FlySample", "Task Cancellable at " + i);
            postProgress(i);
            Thread.sleep(100);
        }
    }

    @Nullable
    @Override
    protected Success buildSuccessEvent() {
        return new Success();
    }

    @Nullable
    @Override
    protected Failure buildFailureEvent() {
        return new Failure();
    }

    @Nullable
    @Override
    protected Progress buildProgressEvent(@SuppressWarnings("UnusedParameters") int progress) {
        return new Progress(progress);
    }

    @Override
    protected boolean isCancellable() {
        return true;
    }

    @Override
    protected void onCancelled() {
        Log.i("FlySample", "Task Cancellable cancelled, cleaning up...");
    }
}
