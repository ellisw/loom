package com.nbarraille.loom;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nbarraille.loom.events.Event;
import com.nbarraille.loom.listeners.LoomListener;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<Integer, WeakReference<Task>> mCurrentTasks;

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

        public Builder setBus(EventBus eventbus) {
            mConfig.setBus(eventbus);
            return this;
        }

        public TaskManager build() {
            EventBus eventBus = mConfig.mEventBus == null ? DEFAULT_BUS : mConfig.mEventBus;
            Executor executor = mConfig.mExecutor == null ? DEFAULT_EXECUTOR :mConfig.mExecutor;
            return new TaskManager(executor, eventBus);
        }
    }

    protected TaskManager(Executor executor, EventBus eventBus) {
        mCurrentTasks = new ConcurrentHashMap<>();
        mExecutor = executor;
        mEventBus = eventBus;
    }
    /**
     * Cancels the task with the given ID. If no task with the given ID exists, this will have no
     * effect
     * @param taskId the ID of the task
     * @throws IllegalStateException if the task with the given ID is not cancellable
     */
    public void cancelTask(int taskId) throws IllegalStateException {
        WeakReference<Task> ref = mCurrentTasks.remove(taskId);
        if (ref != null) {
            Task task = ref.get();
            if (task != null) {
                task.cancel();
            }
        }
    }

    public int execute(final Task task) {
        final int taskId = task.getId();
        mCurrentTasks.put(taskId, new WeakReference<>(task));
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    runTask(task);
                } finally {
                    mCurrentTasks.remove(taskId);
                }
            }
        });
        return taskId;
    }

    public void registerListener(LoomListener listener) {
        mEventBus.register(listener);
    }

    public void unregisterListener(LoomListener listener) {
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
            task.onCancelled();
            return;
        } catch (Exception e) {
            postEvent(task, task.buildFailureEvent());
            return;
        }

        postEvent(task, task.buildSuccessEvent());
    }
}
