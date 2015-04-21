package com.nbarraille.loom;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nbarraille.loom.events.Event;
import com.nbarraille.loom.listeners.LoomListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import de.greenrobot.event.EventBus;

/**
 * TaskManager manages the tasks and takes care of executing them on the appropriate Executor
 */
public class TaskManager {
    // By default Loom will execute tasks in the default AsyncTask thread pool
    private static final Executor DEFAULT_EXECUTOR = AsyncTask.THREAD_POOL_EXECUTOR;
    private static final EventBus DEFAULT_BUS = EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();

    private final Executor mExecutor; // The executor on which the tasks will be executed
    private final EventBus mEventBus; // The EventBus used to notify the listeners
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
        public Builder setLoggingEnabled(boolean loggingEnabled) {
            mConfig.setLoggingEnabled(loggingEnabled);
            return this;
        }

        public TaskManager build() {
            EventBus eventBus = mConfig.mEventBus == null ? DEFAULT_BUS : mConfig.mEventBus;
            Executor executor = mConfig.mExecutor == null ? DEFAULT_EXECUTOR :mConfig.mExecutor;

            boolean loggingEnabled = mConfig.mLoggingEnabled;
            return new TaskManager(executor, eventBus, loggingEnabled);
        }
    }

    protected TaskManager(Executor executor, EventBus eventBus, boolean loggingEnabled) {
        mCurrentTasksById = new HashMap<>();
        mCurrentTasksIds = new HashMap<>();
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
     * Executes a task in the background. The task will run on this TaskManager's executor.
     *
     * @param task the task to execute, cannot be null
     * @return the ID of that task, that can be used later on to cancel it if necessary
     */
    public int execute(@NonNull final Task task) {
        final int taskId = task.getId();
        final String taskName = task.name();
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
            try {
                task.onFailure(e);
            } catch (Exception e1) {
                if (mIsLoggingEnabled) {
                    Log.e(Loom.LOG_TAG, "Error while performing onFailure(): " + e1.getMessage(), e1);
                }
            }
            postEvent(task, task.buildFailureEvent());
            return;
        }
        try {
            task.onSuccess();
        } catch (Exception e) {
            if (mIsLoggingEnabled) {
                Log.e(Loom.LOG_TAG, "Error while performing onSuccess(): " + e.getMessage(), e);
            }
        }
        postEvent(task, task.buildSuccessEvent());
    }
}
