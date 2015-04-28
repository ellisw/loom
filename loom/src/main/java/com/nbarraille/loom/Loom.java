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
 * <code>Loom</code> let's you run background tasks and manage them in a very simple, yet customizable, way.
 *
 * Use the default instance by calling the static methods of this class:
 * @see #execute
 * @see #registerListener
 *
 * You can configure the default instance with {@link #configureDefault}. For this to take effect,
 * you need to configure it <b>before</b> starting using it. We recommend doing so in
 * <code>Application#onCreate</code>.
 *
 * To run a {@link Task}, use {@link #execute}.
 * To get callbacks from this Task, create a {@link LoomListener} and use {@link #registerListener}.
 * You can cancel Tasks by using {@link #cancelTask} or {@link #cancelTasks}.
 *
 * You can also create and manage multiple instances of {@link TaskManager}.
 * @see com.nbarraille.loom.TaskManager.Builder
 */
public abstract class Loom {
    protected static final String LOG_TAG = "Loom";
    private static LoomConfig sConfig;
    private static TaskManager sDefaultInstance;

    /**
     * Configures the default Loom instance. This will not have any effect if called after it has
     * already been used.
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
     * Cancels the <code>Task</code> with the given ID. If no task with the given ID exists,
     * this will have no effect.
     * The task needs to be cancellable, in order for this to work.
     * @see Task#isCancellable()
     *
     * @param taskId the ID of the Task
     * @throws IllegalStateException if the task with the given ID is not cancellable
     */
    @SuppressWarnings("unused")
    public static void cancelTask(int taskId) throws IllegalStateException {
        getDefaultTaskManager().cancelTask(taskId);
    }

    /**
     * Cancels all the tasks with the given name.
     * If you only want to cancel one specific <code>Task</code>, use {@link #cancelTask} instead.
     *
     * @param name the name of the Tasks
     * @throws IllegalStateException if one of the task with the given name is not cancellable
     */
    @SuppressWarnings("unused")
    public static void cancelTasks(String name) throws IllegalStateException {
        getDefaultTaskManager().cancelTasks(name);
    }

    /**
     * Retrieves the {@link TaskStatus} of the Task with the given ID. This can return null
     * for two reasons: Either no Task exist or has existed with this ID, or the Task is too old
     * for the size of the backlog and has been evicted already.
     *
     * @param taskId the ID of the Task
     * @return the status of the Task, or null
     */
    @SuppressWarnings("unused")
    @Nullable
    public static TaskStatus getTaskStatus(int taskId) {
        return getDefaultTaskManager().getTaskStatus(taskId);
    }

    /**
     * Executes a Task in the background. The Task will be scheduled to run on the default
     * Loom <code>Executor</code>, and will start as soon as the Executor is ready.
     * The {@link Task#runTask()} will be called.
     *
     * @param task the Task to execute
     * @return the ID of the Task. You can use this ID to retrieve the status of the Task, or to
     * cancel it.
     */
    @SuppressWarnings("unused")
    public static int execute(Task task) {
        return getDefaultTaskManager().execute(task);
    }

    /**
     * Registers a listener with Loom.
     * The listener will receive all the events sent by tasks with a {@link Task#name()} matching
     * their {@link LoomListener#taskName()}
     * If you are interested in past completion events for a given task,
     * use {@link #registerListener(LoomListener, int)} instead.
     *
     * @param listener the listener to register, cannot be null
     */
    public static void registerListener(@NonNull LoomListener listener) {
        getDefaultTaskManager().registerListener(listener);
    }

    /**
     * Registers a listener with Loom.
     * The listener will receive all the events sent by tasks with a {@link Task#name()} matching
     * their {@link LoomListener#taskName()}
     *
     * If the task with the given ID has already finished (and hasn't been cleared from the backlog
     * yet), the listener's {@link LoomListener#onSuccess} or {@link LoomListener#onFailure}
     * callback be called immediately, in the UI thread.
     *
     * It is recommended to use this version of registerListener when task completion events could
     * have been missed (Activity/Fragment re-creation after configuration change, for example)
     *
     * @param listener the listener to register, cannot be null
     * @param taskId   the ID of the task to receive past Success/Failure events for. If that task ID
     *                 refers to a task that has a different {@link Task#name}
     */
    public static void registerListener(@NonNull LoomListener listener, int taskId) {
        getDefaultTaskManager().registerListener(listener, taskId);
    }

    /**
     * Unregisters a listener with Loom.
     * The listener won't receive any more events.
     * @param listener the listener to register, cannot be null
     */
    public static void unregisterListener(LoomListener listener) {
        getDefaultTaskManager().unregisterListener(listener);
    }
}
