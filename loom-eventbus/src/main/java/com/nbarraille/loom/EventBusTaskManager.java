package com.nbarraille.loom;

import java.util.concurrent.Executor;

import de.greenrobot.event.EventBus;

/**
 * An implementation of TaskManager that uses GreenRobot's EventBus to send task callbacks
 */
public class EventBusTaskManager extends TaskManager<EventBusTaskManager.EventBusWrapper> {
    /**
     * An implementation of Bus for GreenRobot's EventBus
     */
    static class EventBusWrapper implements LoomBus {
        private final EventBus mEventBus;

        public EventBusWrapper(EventBus eventBus) {
            mEventBus = eventBus;
        }

        @Override
        public void postEvent(Object event) {
            mEventBus.post(event);
        }

        @Override
        public void register(LoomListener listener) {
            mEventBus.register(listener);
        }

        @Override
        public void unregister(LoomListener listener) {
            mEventBus.unregister(listener);
        }
    }

    private final static EventBus DEFAULT_EVENT_BUS = EventBus.builder().logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    private final static EventBusWrapper DEFAULT_BUS = new EventBusWrapper(DEFAULT_EVENT_BUS);

    /**
     * A TaskManager.Builder implemetation that builds an EventBusTaskManager
     */
    public static class Builder extends TaskManager.Builder<EventBusWrapper> {
        @Override
        public EventBusTaskManager build() {
            Executor executor = mConfig.mExecutor == null ? DEFAULT_EXECUTOR : mConfig.mExecutor;
            EventBusWrapper bus = mConfig.mBus == null ? DEFAULT_BUS : mConfig.mBus;
            return new EventBusTaskManager(executor, bus);
        }
    }

    private EventBusTaskManager(Executor executor, EventBusWrapper bus) {
        super(executor, bus);
    }
}
