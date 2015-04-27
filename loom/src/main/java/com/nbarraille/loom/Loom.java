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

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nbarraille.loom.listeners.LoomListener;

/**
 * Loom let's you run background tasks and manage them in a very simple way.
 */
public abstract class Loom {
    protected static final String LOG_TAG = "Loom";
    private static LoomConfig sConfig;
    private static TaskManager sDefaultInstance;

    /**
     * Configures the default Loom instance. This won't have any effect if called after it's already
     * been used
     * @param config the config
     */
    @SuppressWarnings("unused")
    public static void configureDefault(LoomConfig config) {
        sConfig = config;
    }

    private static synchronized TaskManager getDefaultTaskManager() {
        if (sDefaultInstance == null) {
            TaskManager.Builder builder = new TaskManager.Builder();
            if (sConfig != null) {
                builder.setConfig(sConfig);
            } else {
                // By default Loom executes tasks on the default AsyncTask executor
                builder.setExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            sDefaultInstance = builder.build();
        }
        return sDefaultInstance;
    }

    /**
     * Cancels the task with the given ID. If no task with the given ID exists, this will have no
     * effect
     * @param taskId the ID of the task
     * @throws IllegalStateException if the task with the given ID is not cancellable
     */
    @SuppressWarnings("unused")
    public static void cancelTask(int taskId) throws IllegalStateException {
        getDefaultTaskManager().cancelTask(taskId);
    }

    /**
     * Cancels all the tasks with the given name.
     * @param name the name of the tasks
     * @throws IllegalStateException if one of the task with the given name is not cancellable
     */
    @SuppressWarnings("unused")
    public static void cancelTasks(String name) throws IllegalStateException {
        getDefaultTaskManager().cancelTasks(name);
    }

    /**
     * Returns the status of the task with the given ID. This can return null for two reasons:
     * Either no task exist or has existed with this ID, or the task is too old for the size of the
     * backlog and has been evicted already.
     *
     * @param taskId the ID of the task
     * @return the status of the task, or null
     */
    @SuppressWarnings("unused")
    @Nullable
    public static TaskStatus getTaskStatus(int taskId) {
        return getDefaultTaskManager().getTaskStatus(taskId);
    }

    /**
     * Executes a task in the background with the default Fly instance.
     * @param task the task to execute
     * @return the ID of the task
     */
    @SuppressWarnings("unused")
    public static int execute(final Task task) {
        return getDefaultTaskManager().execute(task);
    }

    /**
     * Registers a listener with Loom.
     * The listener will receive all the events for its task name.
     * If you are interested in past completion events for a given task, use registerListener(listener, taskId)
     *
     * @param listener the listener, cannot be null
     */
    public static void registerListener(@NonNull LoomListener listener) {
        getDefaultTaskManager().registerListener(listener);
    }

    /**
     * Registers a listener with Loom.
     * The listener will receive all the events for its task name.
     *
     * If the task with the given ID has already finished (and hasn't been cleared from the backlog
     * yet), the listener will receive the success or failure callback immediately, in the UI thread.
     *
     * It is recommended to use this version of registerListener when task completion events could
     * have been missed (Activity/Fragment configuration change for example)
     *
     * @param listener the listener to register, cannot be null
     * @param taskId the ID of the task to receive past Success/Failure events for
     */
    public static void registerListener(@NonNull LoomListener listener, int taskId) {
        getDefaultTaskManager().registerListener(listener, taskId);
    }

    /**
     * Unregisters a listener with Loom.
     * The listener will stop receiving events
     * @param listener the listener, cannot be null
     */
    public static void unregisterListener(LoomListener listener) {
        getDefaultTaskManager().unregisterListener(listener);
    }
}
