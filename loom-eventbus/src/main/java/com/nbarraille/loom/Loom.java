package com.nbarraille.loom;

/**
 * Loom let's you run background tasks and manage them in a very simple way.
 */
public abstract class Loom {
    private static LoomConfig<EventBusTaskManager.EventBusWrapper> sConfig;
    private static EventBusTaskManager sDefaultInstance;

    /**
     * Configures the default Loom instance. This won't have any effect if called after it's already
     * been used
     * @param config the config
     */
    @SuppressWarnings("unused")
    public static void configureDefault(LoomConfig<EventBusTaskManager.EventBusWrapper> config) {
        sConfig = config;
    }

    private static synchronized EventBusTaskManager getDefaultTaskManager() {
        if (sDefaultInstance == null) {
            EventBusTaskManager.Builder builder = new EventBusTaskManager.Builder();
            if (sConfig != null) {
                builder.setConfig(sConfig);
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
     * Executes a task in the background with the default Fly instance.
     * @param task the task to execute
     * @return the ID of the task
     */
    @SuppressWarnings("unused")
    public static int execute(final Task task) {
        return getDefaultTaskManager().execute(task);
    }

    public static void registerListener(LoomListener listener) {
        getDefaultTaskManager().registerListener(listener);
    }

    public static void unregisterListener(LoomListener listener) {
        getDefaultTaskManager().unregisterListener(listener);
    }
}
