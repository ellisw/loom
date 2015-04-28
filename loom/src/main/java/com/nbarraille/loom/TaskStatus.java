/*
 * Copyright (C) 2015 Nathan Barraillé (nathan.barraille@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nbarraille.loom;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.SuccessEvent;
import com.nbarraille.loom.listeners.LoomListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the status of a Task in the backlog.
 * This keeps track of execution of past and current Tasks.
 * This keeps the {@link SuccessEvent} or {@link FailureEvent} object for finished Tasks, so that
 * they can be sent to listeners that missed them using
 * {@link TaskManager#registerListener(LoomListener, int)}.
 */
public class TaskStatus {
    @IntDef({PENDING, STARTED, FINISHED, CANCELLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    public static final int PENDING = 0;
    public static final int STARTED = 1;
    public static final int FINISHED = 2;
    public static final int CANCELLED = 3;

    @Status private int mStatus;
    private SuccessEvent mSuccessEvent;
    private FailureEvent mFailureEvent;

    TaskStatus() {
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

    /**
     * @return the status of the Task this represent
     */
    @Status
    public synchronized int getStatus() {
        return mStatus;
    }

    /**
     * @return whether or not the Task this represent is PENDING. Pending means that the Task has
     * been scheduled to be executed, but has not been picked up by the <code>Executor</code> yet.
     */
    public synchronized boolean isPending() {
        return mStatus == PENDING;
    }

    /**
     * @return whether or not the Task this represent is STARTED. Started means that the Task has
     * started running on the <code>Executor</code> and has not finished yet.
     */
    public synchronized boolean isStarted() {
        return mStatus == STARTED;
    }

    /**
     * @return whether or not the Task this represent is FINISHED. Finished means that the Task has
     * finished its execution, successfully or not, and has not been cancelled
     */
    public synchronized boolean isFinished() {
        return mStatus == FINISHED;
    }

    /**
     * @return whether or not the Task this represent is CANCELLED. Cancelled means that the Task has
     * been cancelled at some point.
     */
    public synchronized boolean isCancelled() {
        return mStatus == CANCELLED;
    }

    /**
     * @return the SuccessEvent sent by the Task this represents. If the task has not finished or
     * failed, this will be null.
     */
    @Nullable
    public synchronized SuccessEvent getSuccessEvent() {
        return mSuccessEvent;
    }

    /**
     * @return the FailureEvent sent by the Task this represents. If the task has not finished or
     * succeeded, this will be null.
     */
    @Nullable
    public synchronized FailureEvent getFailureEvent() {
        return mFailureEvent;
    }
}
