package com.nbarraille.loom;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;
import com.nbarraille.loom.listeners.BackgroundListener;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple EventCatcher that keep tracks of the event is received for testing purposes.
 * It will automatically make test fail if multiple success/failure events are received
 */
public class EventCatcher <Success extends SuccessEvent, Failure extends FailureEvent, Progress extends ProgressEvent>
        extends BackgroundListener<Success, Failure, Progress> {
    private final String mTaskName;
    @Nullable private Success mReceivedSuccess;
    @Nullable private Failure mReceivedFailure;
    private final List<Progress> mReceivedProgresses;

    public EventCatcher(String taskName) {
        mTaskName = taskName;
        mReceivedProgresses = new ArrayList<>();
    }

    @Override
    public void onSuccess(Success event) {
        if (mReceivedSuccess != null || mReceivedFailure != null) {
            Assert.fail("Duplicate success/failure event received");
        }
        mReceivedSuccess = event;
    }

    @Override
    public void onFailure(Failure event) {
        if (mReceivedSuccess != null || mReceivedFailure != null) {
            Assert.fail("Duplicate success/failure event received");
        }
        mReceivedFailure = event;
    }

    @Override
    public void onProgress(Progress event) {
        if (mReceivedSuccess != null || mReceivedFailure != null) {
            Assert.fail("Progress event received after success/failure");
        }
        mReceivedProgresses.add(event);
    }

    @NonNull
    @Override
    public String taskName() {
        return mTaskName;
    }

    @Nullable
    public Success getReceivedSuccess() {
        return mReceivedSuccess;
    }

    @Nullable
    public Failure getReceivedFailure() {
        return mReceivedFailure;
    }

    public List<Progress> getReceivedProgresses() {
        return new ArrayList<>(mReceivedProgresses);
    }
}
