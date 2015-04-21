package com.nbarraille.loom.events;

/**
 * The base class for all the task events
 */
public abstract class Event {
    private String mTaskName;

    public void setTaskName(String taskName) {
        mTaskName = taskName;
    }

    public String getTaskName() {
        return mTaskName;
    }
}
