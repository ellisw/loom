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
