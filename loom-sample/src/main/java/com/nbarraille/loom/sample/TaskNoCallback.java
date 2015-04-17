package com.nbarraille.loom.sample;

import android.util.Log;

import com.nbarraille.loom.Task;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * The simple task possible, this doesn't send any callback to any listeners and is not cancellable
 */
public class TaskNoCallback extends Task<SuccessEvent, FailureEvent, ProgressEvent> {
    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            Log.i("FlySample", "Task NoCallback at " + i);
            postProgress(i);
            Thread.sleep(100);
        }
    }
}
