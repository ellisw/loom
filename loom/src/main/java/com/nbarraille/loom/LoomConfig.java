package com.nbarraille.loom;

import java.util.concurrent.Executor;

import de.greenrobot.event.EventBus;

/**
 * The configuration of a TaskManager, with a fluent API.
 */
public class LoomConfig {
    private final static int DEFAULT_MAX_BACKLOG_SIZE = 1024;
    protected Executor mExecutor;
    protected EventBus mEventBus;
    protected boolean mLoggingEnabled = false;
    protected int mMaxBacklogSize = DEFAULT_MAX_BACKLOG_SIZE;

    /**
     * Sets the executor on which the tasks are going to be executed
     * @param executor the executor
     * @return the same config object
     */
    public LoomConfig setExecutor(Executor executor) {
        mExecutor = executor;
        return this;
    }

    /**
     * Sets the bus on which the callback messages are going to be sent
     * @param eventBus the bus
     * @return the same config object
     */
    public LoomConfig setBus(EventBus eventBus) {
        mEventBus = eventBus;
        return this;
    }

    /**
     * Sets the max number of tasks to keep in the backlog.
     * @param maxBacklogSize the size
     * @return the same config object
     */
    public LoomConfig setMaxBacklogSize(int maxBacklogSize) {
        mMaxBacklogSize = maxBacklogSize;
        return this;
    }

    /**
     * Enables/Disables logs.
     * Logs are disabled by default
     * @param enabled true for enabling logging, false for disabling it
     * @return the same config object
     */
    public LoomConfig setLoggingEnabled(boolean enabled) {
        mLoggingEnabled = enabled;
        return this;
    }
}
