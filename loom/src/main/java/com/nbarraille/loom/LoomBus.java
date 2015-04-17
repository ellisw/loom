package com.nbarraille.loom;

/**
 * A Bus on which tasks progress can be sent and on which listeners can register
 */
public interface LoomBus {
    void postEvent(Object event);

    void register(LoomListener listener);

    void unregister(LoomListener listener);
}
