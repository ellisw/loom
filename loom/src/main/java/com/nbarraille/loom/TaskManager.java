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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.SuccessEvent;
import com.nbarraille.loom.listeners.LoomListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;

/**
 * A Manager for running Tasks in the background.
 *
 * Creates an instance with {@link com.nbarraille.loom.TaskManager.Builder}, or use the default one
 * with {@link Loom}.
 *
 * To run a {@link Task}, use {@link #execute}.
 * To get callbacks from this Task, create a {@link LoomListener} and use {@link #registerListener}.
 * You can cancel Tasks by using {@link #cancelTask} or {@link #cancelTasks}.
 */
public class TaskManager {
    private final Executor mExecutor; // The executor on which the tasks will be executed
    private final EventBus mEventBus; // The EventBus used to notify the listeners
    private final LruCache<Integer, TaskStatus> mTaskStatuses; // Keeping track of the status of all current and past tasks for this session
    private final Map<Integer, WeakReference<Task>> mCurrentTasksById;
    private final Map<String, Set<Integer>> mCurrentTasksIds;
    private final boolean mIsLoggingEnabled;

    /**
     * Builder with fluent API to build <code>TaskManager</code> objects
     */
    public static class Builder {
        protected LoomConfig mConfig;

        /**
         * Creates a new Builder
         */
        public Builder() {
            mConfig = new LoomConfig();
        }

        /**
         * Sets a new {@link LoomConfig} for this Builder. This will override all the previously set
         * parameters for this <code>Builder</code>.
         *
         * @param config the config
         * @return the same Builder object
         */
        @SuppressWarnings("unused")
        public Builder setConfig(@NonNull LoomConfig config) {
            mConfig = config;
            return this;
        }

        /**
         * Sets the {@link Executor} for the Tasks to run on.
         *
         * @param executor the Executor
         * @return the same Builder object
         */
        @SuppressWarnings("unused")
        public Builder setExecutor(Executor executor) {
            mConfig.setExecutor(executor);
            return this;
        }

        /**
         * Sets the {@link EventBus} on which the Success, Failure and Progress events will be send
         * back to the Listeners.
         *
         * @param eventBus the bus
         * @return the same Builder object
         */
        @SuppressWarnings("unused")
        public Builder setBus(EventBus eventBus) {
            mConfig.setBus(eventBus);
            return this;
        }

        /**
         * Sets the size of the backlog. This is the number of Tasks Loom can keep track of.
         * The number of running/pending tasks can exceed that number, this will only affect
         * {@link TaskManager#getTaskStatus} and {@link TaskManager#registerListener(LoomListener, int)}.
         * This is configured to <code>1024</code> by default.
         *
         * @param maxBacklogSize the size of the backlog
         * @return the same Builder object
         */
        @SuppressWarnings("unused")
        public Builder setMaxBacklogSize(int maxBacklogSize) {
            mConfig.setMaxBacklogSize(maxBacklogSize);
            return this;
        }

        /**
         * Sets whether or not the TaskManager will log non-fatal errors or not.
         * This is false by default.
         *
         * @param enabled whether or not the logging is enabled
         * @return the same Builder object
         */
        @SuppressWarnings("unused")
        public Builder setLoggingEnabled(boolean enabled) {
            mConfig.setLoggingEnabled(enabled);
            return this;
        }

        private static Executor buildDefaultExecutor() {
            return Executors.newFixedThreadPool(2);
        }

        private static EventBus buildDefaultBus() {
            return EventBus.builder().logNoSubscriberMessages(false)
                    .sendNoSubscriberEvent(false).build();
        }

        /**
         * @return the TaskManager configured with this Builder
         */
        public TaskManager build() {
            EventBus eventBus = mConfig.mEventBus == null ? buildDefaultBus() : mConfig.mEventBus;
            Executor executor = mConfig.mExecutor == null ? buildDefaultExecutor() : mConfig.mExecutor;

            boolean loggingEnabled = mConfig.mLoggingEnabled;
            return new TaskManager(executor, eventBus, loggingEnabled, mConfig.mMaxBacklogSize);
        }
    }

    private TaskManager(Executor executor, EventBus eventBus, boolean loggingEnabled,
                        int maxBacklogSize) {
        mCurrentTasksById = new HashMap<>();
        mCurrentTasksIds = new HashMap<>();
        mTaskStatuses = new LruCache<>(maxBacklogSize);
        mExecutor = executor;
        mEventBus = eventBus;
        mIsLoggingEnabled = loggingEnabled;
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
    public void cancelTask(int taskId) throws IllegalStateException {
        Task task = null;
        synchronized (mCurrentTasksById) {
            WeakReference<Task> ref = mCurrentTasksById.remove(taskId);
            if (ref != null) {
                task = ref.get();
                Set<Integer> taskIds = mCurrentTasksIds.get(task.name());
                if (taskIds != null) {
                    taskIds.remove(taskId);
                }
            }
        }

        if (task != null) {
            task.cancel();
            TaskStatus status = mTaskStatuses.get(taskId);
            if (status != null) {
                status.setCancelled();
            }
        }
    }

    /**
     * Cancels all the tasks with the given name.
     * If you only want to cancel one specific <code>Task</code>, use {@link #cancelTask} instead.
     *
     * @param name the name of the Tasks
     * @throws IllegalStateException if one of the task with the given name is not cancellable
     */
    public void cancelTasks(String name) {
        synchronized (mCurrentTasksById) {
            Set<Integer> taskIds = mCurrentTasksIds.get(name);
            if (taskIds != null) {
                for (int taskId : taskIds) {
                    cancelTask(taskId);
                }
            }
        }
    }

    /**
     * Retrieves the {@link TaskStatus} of the Task with the given ID. This can return null
     * for two reasons: Either no Task exist or has existed with this ID, or the Task is too old
     * for the size of the backlog and has been evicted already.
     * @see com.nbarraille.loom.TaskManager.Builder#setMaxBacklogSize
     *
     * @param taskId the ID of the Task
     * @return the status of the Task, or null
     */
    @Nullable
    public TaskStatus getTaskStatus(int taskId) {
        return mTaskStatuses.get(taskId);
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
    public int execute(@NonNull final Task task) {
        final int taskId = task.getId();
        final String taskName = task.name();
        mTaskStatuses.put(taskId, new TaskStatus());
        synchronized (mCurrentTasksById) {
            mCurrentTasksById.put(taskId, new WeakReference<>(task));
            Set<Integer> taskIds = mCurrentTasksIds.get(taskName);
            if (taskIds == null) {
                taskIds = new HashSet<>();
                mCurrentTasksIds.put(taskName, taskIds);
            }
            taskIds.add(taskId);
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runTask(task);
                } finally {
                    synchronized (mCurrentTasksById) {
                        mCurrentTasksById.remove(taskId);
                        Set<Integer> taskIds = mCurrentTasksIds.get(taskName);
                        if (taskIds != null) {
                            taskIds.remove(taskId);
                        }
                    }
                }
            }
        });
        return taskId;
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
    public void registerListener(@NonNull LoomListener listener) {
        mEventBus.register(listener);
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
    public void registerListener(@NonNull final LoomListener listener, int taskId) {
        mEventBus.register(listener);
        TaskStatus status = getTaskStatus(taskId);
        if (status != null) {
            if (status.isFinished()) {
                final SuccessEvent success = status.getSuccessEvent();
                final FailureEvent failure = status.getFailureEvent();
                if (success != null) {
                    if (! TextUtils.equals(success.getTaskName(), listener.taskName()) && mIsLoggingEnabled) {
                        Log.e(Loom.LOG_TAG, "The task with id " + taskId + " is not of type " + listener.taskName());
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection unchecked
                            listener.onSuccess(success);
                        }
                    });
                } else if (failure != null) {
                    if (! TextUtils.equals(failure.getTaskName(), listener.taskName()) && mIsLoggingEnabled) {
                        Log.e(Loom.LOG_TAG, "The task with id " + taskId + " is not of type " + listener.taskName());
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            //noinspection unchecked
                            listener.onFailure(failure);
                        }
                    });
                }
            }
        }
    }

    /**
     * Unregisters a listener with Loom.
     * The listener won't receive any more events.
     * @param listener the listener to register, cannot be null
     */
    public void unregisterListener(@NonNull LoomListener listener) {
        mEventBus.unregister(listener);
    }

    /**
     * @return the Executor used by this TaskManager
     */
    Executor getExecutor() {
        return mExecutor;
    }

    final void postEvent(Task task, @Nullable Event event) {
        if (event != null) {
            event.setTaskName(task.name());
            mEventBus.post(event);
        }
    }

    private void runTask(@NonNull Task task) {
        if (task.isCancelled()) {
            return;
        }
        final TaskStatus status = mTaskStatuses.get(task.getId());
        if (status != null) {
            status.setStarted();
        }
        try {
            task.run(this);
        } catch (InterruptedException e) {
            // The task has been interrupted
            try {
                task.onCancelled();
            } catch (Exception e1) {
                if (mIsLoggingEnabled) {
                    Log.e(Loom.LOG_TAG, "Error while performing onCancelled(): " + e1.getMessage(), e1);
                }
            }
            return;
        } catch (Exception e) {
            FailureEvent failureEvent = task.buildFailureEvent();
            try {
                if (status != null) {
                    status.setFailure(failureEvent);
                }
                task.onFailure(e);
            } catch (Exception e1) {
                if (mIsLoggingEnabled) {
                    Log.e(Loom.LOG_TAG, "Error while performing onFailure(): " + e1.getMessage(), e1);
                }
            }
            postEvent(task, failureEvent);
            return;
        }
        SuccessEvent successEvent = task.buildSuccessEvent();
        try {
            if (status != null) {
                status.setSuccess(successEvent);
            }
            task.onSuccess();
        } catch (Exception e) {
            if (mIsLoggingEnabled) {
                Log.e(Loom.LOG_TAG, "Error while performing onSuccess(): " + e.getMessage(), e);
            }
        }
        postEvent(task, successEvent);
    }
}
