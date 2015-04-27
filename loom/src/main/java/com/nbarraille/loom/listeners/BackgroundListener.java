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
package com.nbarraille.loom.listeners;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener for GreenRobot's EventBus that receives callbacks on the same background thread
 * where the task was running
 */
public abstract class BackgroundListener<Success extends SuccessEvent, Failure extends FailureEvent, Progress extends ProgressEvent>
        implements LoomListener<Success, Failure, Progress> {
    @SuppressWarnings("unused")
    public final void onEvent(SuccessEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onSuccess((Success) event);
            } catch (ClassCastException e) {}
        }
    }

    @SuppressWarnings("unused")
    public final void onEvent(FailureEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onFailure((Failure) event);
            } catch (ClassCastException e) {}
        }
    }

    @SuppressWarnings("unused")
    public final void onEvent(ProgressEvent event) {
        if (taskName().equals(event.getTaskName())) {
            //noinspection EmptyCatchBlock
            try {
                //noinspection unchecked
                onProgress((Progress) event);
            } catch (ClassCastException e) {}
        }
    }

    @Override
    public void onSuccess(Success event) {}

    @Override
    public void onFailure(Failure event) {}

    @Override
    public void onProgress(Progress event) {}
}
