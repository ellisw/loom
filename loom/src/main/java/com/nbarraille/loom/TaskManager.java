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

import com.nbarraille.loom.events.Event;
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
 * TaskManager manages the tasks and takes care of executing them on the appropriate Executor
 */
public class TaskManager {
    private final Executor mExecutor; // The executor on which the tasks will be executed
    private final EventBus mEventBus; // The EventBus used to notify the listeners
    private final LruCache<Integer, TaskStatus> mTaskStatuses; // Keeping track of the status of all current and past tasks for this session
    private final Map<Integer, WeakReference<Task>> mCurrentTasksById;
    private final Map<String, Set<Integer>> mCurrentTasksIds;
    private final boolean mIsLoggingEnabled;

    /**
     * Builder with fluent API to build TaskManager objects
     */
    public static class Builder {
        protected LoomConfig mConfig;

        public Builder() {
            mConfig = new LoomConfig();
        }

        public Builder setConfig(@NonNull LoomConfig config) {
            mConfig = config;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            mConfig.setExecutor(executor);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setBus(EventBus eventbus) {
            mConfig.setBus(eventbus);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setMaxBacklogSize(int maxBacklogSize) {
            mConfig.setMaxBacklogSize(maxBacklogSize);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setLoggingEnabled(boolean loggingEnabled) {
            mConfig.setLoggingEnabled(loggingEnabled);
            return this;
        }

        private static Executor buildDefaultExecutor() {
            return Executors.newFixedThreadPool(2);
        }

        private static EventBus buildDefaultBus() {
            return EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
        }

        public TaskManager build() {
            EventBus eventBus = mConfig.mEventBus == null ? buildDefaultBus() : mConfig.mEventBus;
            Executor executor = mConfig.mExecutor == null ? buildDefaultExecutor() : mConfig.mExecutor;

            boolean loggingEnabled = mConfig.mLoggingEnabled;
            return new TaskManager(executor, eventBus, loggingEnabled, mConfig.mMaxBacklogSize);
        }
    }

    protected TaskManager(Executor executor, EventBus eventBus, boolean loggingEnabled, int maxBacklogSize) {
        mCurrentTasksById = new HashMap<>();
        mCurrentTasksIds = new HashMap<>();
        mTaskStatuses = new LruCache<>(maxBacklogSize);
        mExecutor = executor;
        mEventBus = eventBus;
        mIsLoggingEnabled = loggingEnabled;
    }
    /**
     * Cancels the task with the given ID. If no task with the given ID exists, this will have no
     * effect
     * @param taskId the ID of the task
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
     * Cancels all the tasks (scheduled or running) for the given name.
     * @param name the name of the task
     * @throws IllegalStateException if one of the task with that name is not cancellable
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
     * Returns the status of the task with the given ID. This can return null for two reasons:
     * Either no task exist or has existed with this ID, or the task is too old for the size of the
     * backlog and has been evicted already.
     *
     * @param taskId the ID of the task
     * @return the status of the task, or null
     */
    @Nullable
    public TaskStatus getTaskStatus(int taskId) {
        return mTaskStatuses.get(taskId);
    }

    /**
     * Executes a task in the background. The task will run on this TaskManager's executor.
     *
     * @param task the task to execute, cannot be null
     * @return the ID of that task, that can be used later on to cancel it if necessary
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
     * Registers a listener with this instance of TaskManager.
     * The listener will receive all the events for its task name
     * @param listener the listener, cannot be null
     */
    public void registerListener(@NonNull LoomListener listener) {
        mEventBus.register(listener);
    }

    /**
     * Registers a listener with this instance of TaskManager.
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
     * Unregisters a listener with this instance of TaskManager.
     * The listener will stop receiving events
     * @param listener the listener, cannot be null
     */
    public void unregisterListener(@NonNull LoomListener listener) {
        mEventBus.unregister(listener);
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

    /**
     * @return the executor used by this task manager
     */
    public Executor getExecutor() {
        return mExecutor;
    }


}
