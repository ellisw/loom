package com.nbarraille.loom.sample;

import android.support.annotation.Nullable;
import android.util.Log;

import com.nbarraille.loom.Task;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;


/**
 * Simple task that reports its success and failure to its listeners but no progress
 */
public class TaskNoProgress extends Task<TaskNoProgress.Success, TaskNoProgress.Failure, ProgressEvent> {
    public static class Success extends SuccessEvent {}
    public static class Failure extends FailureEvent {}

    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            Log.i("FlySample", "Task NoProgress at " + i);
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
}
