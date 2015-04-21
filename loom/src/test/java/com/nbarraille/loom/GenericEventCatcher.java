package com.nbarraille.loom;


import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * Simple EventCatcher that keep tracks of the event is received for testing purposes.
 * It will automatically make test fail if multiple success/failure events are received
 */
public class GenericEventCatcher extends EventCatcher<SuccessEvent, FailureEvent, ProgressEvent> {
    public GenericEventCatcher(String taskName) {
        super(taskName);
    }
}
