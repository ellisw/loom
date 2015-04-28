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

import com.nbarraille.loom.listeners.LoomListener;

import java.util.concurrent.Executor;

import de.greenrobot.event.EventBus;

/**
 * The configuration of a <code>TaskManager</code>, with a fluent API.
 */
public class LoomConfig {
    private final static int DEFAULT_MAX_BACKLOG_SIZE = 1024;
    protected Executor mExecutor;
    protected EventBus mEventBus;
    protected boolean mLoggingEnabled = false;
    protected int mMaxBacklogSize = DEFAULT_MAX_BACKLOG_SIZE;

    /**
     * Sets the {@link Executor} for the Tasks to run on.
     *
     * @param executor the Executor
     * @return the same LoomConfig object
     */
    public LoomConfig setExecutor(Executor executor) {
        mExecutor = executor;
        return this;
    }

    /**
     * Sets the {@link EventBus} on which the Success, Failure and Progress events will be send
     * back to the Listeners.
     *
     * @param eventBus the bus
     * @return the same LoomConfig object
     */
    public LoomConfig setBus(EventBus eventBus) {
        mEventBus = eventBus;
        return this;
    }

    /**
     * Sets the size of the backlog. This is the number of Tasks Loom can keep track of.
     * The number of running/pending tasks can exceed that number, this will only affect
     * {@link TaskManager#getTaskStatus} and {@link TaskManager#registerListener(LoomListener, int)}.
     * This is configured to <code>1024</code> by default.
     *
     * @param maxBacklogSize the size of the backlog
     * @return the same LoomConfig object
     */
    public LoomConfig setMaxBacklogSize(int maxBacklogSize) {
        mMaxBacklogSize = maxBacklogSize;
        return this;
    }

    /**
     * Sets whether or not the TaskManager will log non-fatal errors or not.
     * This is false by default.
     *
     * @param enabled whether or not the logging is enabled
     * @return the same Builder object
     */
    public LoomConfig setLoggingEnabled(boolean enabled) {
        mLoggingEnabled = enabled;
        return this;
    }
}
