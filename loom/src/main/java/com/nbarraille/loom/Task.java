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

import android.support.annotation.Nullable;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A Task to be executed in the background.
 *
 * Create your own Task by subclassing this and overriding {@link #name()} and {@link #runTask}.
 *
 * It is recommended to create top-level classes or <b>static</b> inner classes for your Tasks
 * in order to avoid leaking the outer class (often an <code>Activity</code> or <code>Fragment</code>)
 * while this Task is running.
 *
 * This Task should be executed by running {@link TaskManager#execute} or {@link Loom#execute}.
 *
 * The Task is considered successful if <code>runTask</code> returns, or failed if
 * <code>runTask</code> raises an Exception.
 * The <code>TaskManager</code> will automatically send a {@link SuccessEvent} or {@link FailureEvent}
 * accordingly.
 * You need to create a {@link com.nbarraille.loom.listeners.LoomListener} with a
 * <code>taskName</code> matching this Task's <code>name</code> and register it with the
 * <code>TaskManager</code> to receive those events.
 *
 * You can customize the <code>SuccessEvent</code> and <code>FailureEvent</code> by overriding the
 * {@link #buildSuccessEvent} and/or {@link #buildFailureEvent} methods.
 * The recommended way to do this is to create your own subclasses of <code>SuccessEvent</code>
 * and/or <code>FailureEvent</code>, and have the methods return a new instance of them.
 *
 * This Task can also returns its progress. For this, it is up to you to call {@link #postProgress}
 * from the <code>runTask</code> method. By default, a generic {@link ProgressEvent} will be sent,
 * but you can also customize it by overriding {@link #buildProgressEvent}.
 *
 * By default, Tasks are not cancellable, so that they will not exit in an indefinite state.
 * If you want your Task to be cancellable, you need to override the {@link #isCancellable} method
 * to return <code>true</code>. To make sure your Task cleans up after itself when cancelled, you
 * can override the {@link #onCancelled} method, which will be executed in the same Thread as the
 * <code>runTask</code> method. <code>onCancelled</code> will only be called if the Task has been
 * cancelled <b>after</b> its execution has started.
 *
 * The Task also provides {@link #onSuccess} and {@link #onFailure} callbacks that will be called in
 * the same Thread as the <code>runTask</code> method after the success/failure event is sent.
 *
 */
public abstract class Task {
    private TaskManager mManager; // The manager this Task is executed with
    @Nullable private volatile Thread mThread; // The thread on which that task is running. Will be null until it starts executing
    private volatile boolean mIsCancelled = false; // Whether or not that task has been cancelled
    private volatile boolean mIsFinished = false; // Whether or not that task has been cancelled

    /**
     * @return the ID of the Task
     */
    final int getId() {
        return hashCode();
    }

    /**
     * The name of the Task, used to match the Task's events with the right listeners.
     *
     * @return the name of the task
     */
    protected abstract String name();

    /**
     * The actual code to be executed in the background.
     * This method will be executed on the <code>TaskManager</code>'s <code>Executor</code>.
     * The Task will be assumed to be successful if this method returns, or failed if it throws an
     * Exception.
     * You can also call {@link #postProgress} at any point in this method to send progress to
     * listeners.
     *
     * @throws Exception when the task fails
     */
    protected abstract void runTask() throws Exception;

    /**
     * Builds the event to be sent on the bus when the task succeeds. By default this builds a generic
     * {@link SuccessEvent}.
     *
     * Override this if you want to send custom events.
     * @return the Event to be sent
     */
    @Nullable
    protected SuccessEvent buildSuccessEvent() {
        return new SuccessEvent();
    }

    /**
     * Builds the event to be sent on the bus when the task fails. By default this builds a generic
     * {@link FailureEvent}.
     *
     * Override this if you want to send custom events.
     * @return the Event to be sent
     */
    @Nullable
    protected FailureEvent buildFailureEvent() {
        return new FailureEvent();
    }

    /**
     * Builds the event to be sent on the bus when the task progresses. By default this builds a generic
     * {@link ProgressEvent}.
     *
     * Override this if you want to send custom events.
     * @return the Event to be sent
     */
    @Nullable
    protected ProgressEvent buildProgressEvent(int progress) {
        return new ProgressEvent(progress);
    }

    /**
     * Callback for subclasses to implement being executed when the task succeeds.
     * <code>SuccessEvent</code>s are automatically sent by the <code>TaskManager</code>.
     * You <b>do not</b> need to send them here.
     *
     * This callback will be executed in the same Thread as <code>runTask</code>.
     */
    protected void onSuccess() {}

    /**
     * Callback for subclasses to implement being executed when the task fails.
     * <code>FailureEvent</code>s are automatically sent by the <code>TaskManager</code>.
     * You <b>do not</b> need to send them here.
     *
     * This callback will be executed in the same Thread as <code>runTask</code>.
     */
    protected void onFailure(@SuppressWarnings("UnusedParameters") Exception error) {}

    /**
     * Callback for subclasses to implement being executed when the task is cancelled after its
     * execution has started.
     * You can do all the clean up for a Task that did not complete here.
     *
     * This callback will be executed in the same Thread as <code>runTask</code>.
     */
    protected void onCancelled() {}

    /**
     * Posts a progress event on the event bus.
     * This method should be called by subclasses whenever they want to report their progress.
     * <b>This should only be called from <code>runTask</code></b>.
     * You can customize the {@link ProgressEvent} being sent by overriding {@link #buildProgressEvent}.
     *
     * @param progress an integer representing the progress of the task, must be between 0 and 100
     */
    @SuppressWarnings("unused")
    protected final void postProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Invalid progress: " + progress);
        }
        if (isFinished()) {
            throw new IllegalStateException("Cannot send progress for a finished task");
        }
        if (isCancelled()) {
            // Listeners don't want to be noticed about progress after a task has been cancelled
            return;
        }

        ProgressEvent event = buildProgressEvent(progress);
        if (event != null && mManager != null) {
            mManager.postEvent(this, event);
        }
    }

    /**
     * Tasks are not cancellable by default.
     * Subclasses need to override this to return <code>true</code> if they want the Task to be
     * cancellable.
     * If they do, they can also override {@link #onCancelled} to cleanup after the cancellation.
     *
     * @return whether or not the Task is cancellable
     */
    protected boolean isCancellable() {
        return false;
    }

    /**
     * @return whether or not the Task has been cancelled
     */
    protected boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * @return whether or not the Task has finished
     */
    protected boolean isFinished() {
        return mIsFinished;
    }

    final void run(TaskManager manager) throws Exception {
        mThread = Thread.currentThread();
        mManager = manager;
        try {
            runTask();
        } finally {
            mIsFinished = true;
            mThread = null;
        }
    }

    final void cancel() throws IllegalStateException {
        if (!isCancellable()) {
            throw new IllegalStateException("The task is not cancellable");
        }
        if (mIsCancelled) {
            return;
        }
        mIsCancelled = true;
        // We have to use a local variable, because mThread is volatile and might change
        Thread thread = mThread;
        if (thread != null) {
            thread.interrupt();
        }
    }
}
