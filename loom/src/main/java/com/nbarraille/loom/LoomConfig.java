package com.nbarraille.loom;

import java.util.concurrent.Executor;

/**
 * The configuration of a TaskManager, with a fluent API.
 */
public class LoomConfig<Bus> {
    protected Executor mExecutor;
    protected Bus mBus;

    /**
     * Sets the executor on which the tasks are going to be executed
     * @param executor the executor
     * @return the same config object
     */
    public LoomConfig setExecutor(Executor executor) {
        mExecutor = executor;
        return this;
    }

    /**
     * Sets the bus on which the callback messages are going to be sent
     * @param bus the bus
     * @return the same config object
     */
    public LoomConfig setBus(Bus bus) {
        mBus = bus;
        return this;
    }
}
