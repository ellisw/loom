package com.nbarraille.loom;

import android.support.annotation.Nullable;

/**
 * A task to be executed in the background
 */
public abstract class Task<Success, Failure, Progress> {
    private TaskManager mManager;
    @Nullable
    private volatile Thread mThread; // The thread on which that task is running. Will be null until it starts executing
    private volatile boolean mIsCancelled = false; // Whether or not that task has been cancelled

    /**
     * @return the ID of the task
     */
    public int getId() {
        return hashCode();
    }

    /**
     * The task to be executed.
     * The task will consider to succeed if that method returns, and to fail if an exception is thrown
     * @throws Exception when the task fails
     */
    protected abstract void runTask() throws Exception;

    /**
     * Builds the event to be sent on the bus when the task succeeds.
     * Subclasses should implement this if they want to notify listeners about successes.
     * @return the event to be sent
     */
    @Nullable
    protected Success buildSuccessEvent() {
        return null;
    }

    /**
     * Builds the event to be sent on the bus when the task fails.
     * Subclasses should implement this if they want to notify listeners about failures.
     * @return the event to be sent
     */
    @Nullable
    protected Failure buildFailureEvent() {
        return null;
    }

    /**
     * Builds the event to be sent on the bus when the task succeeds.
     * Subclasses should implement this if they want to notify listeners about successes.
     * @param progress the progress, will be between 0 and 100 (inclusive)
     * @return the event to be sent
     */
    @Nullable
    protected Progress buildProgressEvent(@SuppressWarnings("UnusedParameters") int progress) {
        return null;
    }

    /**
     * Callback for subclasses to implement being executed when the task finishes successfully.
     * This callback will be executed in the same background thread as the actual task
     */
    protected void onSuccess() {}

    /**
     * Callback for subclasses to implement being executed when the task fails
     * This callback will be executed in the same background thread as the actual task
     * @param error the error that occurred
     */
    protected void onFailure(@SuppressWarnings("UnusedParameters") Exception error) {}

    /**
     * Callback for subclasses to implement being executed when the task has been cancelled.
     * All the cleanup should be done here
     */
    protected void onCancelled() {}

    /**
     * Runs the background task in the current thread.
     * You should not call this directly, and use BackgroundTaskManager.execute(task) or
     * BackgroundTaskManager.executeOnExecutor(task, executor) to execute that task
     */
    final void run(TaskManager manager) throws Exception {
        mThread = Thread.currentThread();
        mManager = manager;
        try {
            runTask();
        } finally {
            mThread = null;
        }
    }

    /**
     * Posts a progress event on the event bus.
     * This method should be called by subclasses whenever they want to report their progress
     * @param progress an integer representing the progress of the task, must be between 0 and 100
     */
    @SuppressWarnings("unused")
    protected final void postProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Invalid progress: " + progress);
        }
        Progress event = buildProgressEvent(progress);
        if (event != null) {
            mManager.postEvent(event);
        }
    }

    /**
     * BackgroundTasks are not cancellable by default.
     * Subclasses must override this to return true if they want the task to be cancellable.
     * If they do, they probably need to override onCancelled() to cleanup everything.
     * @return whether or not the task is cancellable
     */
    protected boolean isCancellable() {
        return false;
    }

    /**
     * @return whether or not the task has been cancelled
     */
    protected boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Cancels the current task. This should only be called by the BackgroundTaskManager
     * @throws IllegalStateException if the task is not cancellable
     */
    void cancel() throws IllegalStateException {
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
