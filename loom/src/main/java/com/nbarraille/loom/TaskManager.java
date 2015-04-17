package com.nbarraille.loom;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * TaskManager manages the tasks and takes care of executing them on the appropriate Executor
 */
abstract class TaskManager<Bus extends LoomBus> {
    // By default Loom will execute tasks in the default AsyncTask thread pool
    protected static final Executor DEFAULT_EXECUTOR = AsyncTask.THREAD_POOL_EXECUTOR;

    private final Executor mExecutor; // The executor on which the tasks will be executed
    private final Bus mBus;
    private final Map<Integer, WeakReference<Task>> mCurrentTasks;

    /**
     * Builder with fluent API to build TaskManager objects
     */
    public abstract static class Builder<Bus> {
        protected LoomConfig<Bus> mConfig;

        public Builder() {
            mConfig = new LoomConfig<>();
        }

        public Builder setConfig(@NonNull LoomConfig<Bus> config) {
            mConfig = config;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            mConfig.setExecutor(executor);
            return this;
        }

        public Builder setBus(Bus bus) {
            mConfig.setBus(bus);
            return this;
        }

        public abstract TaskManager build();
    }

    protected TaskManager(Executor executor, Bus bus) {
        mCurrentTasks = new ConcurrentHashMap<>();
        mExecutor = executor;
        mBus = bus;
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
        mBus.register(listener);
    }

    public void unregisterListener(LoomListener listener) {
        mBus.unregister(listener);
    }

    final void postEvent(@Nullable Object event) {
        if (event != null) {
            mBus.postEvent(event);
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
            postEvent(task.buildFailureEvent());
            return;
        }

        postEvent(task.buildSuccessEvent());
    }
}
