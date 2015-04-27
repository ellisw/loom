/*
 * Copyright (C) 2015 Nathan Barraillé (nathan.barraille@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * Simple EventCatcher that keeps track of the event it received for testing purposes.
 * It will automatically make tests fail if inconsistent events are received
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
