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
 * A LoomListener for GreenRobot's EventBus that receives callbacks on the UI Thread
 */
public abstract class GenericUiThreadListener implements LoomListener<SuccessEvent, FailureEvent, ProgressEvent> {
    @SuppressWarnings("unused")
    public final void onEventMainThread(SuccessEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onSuccess(event);
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(FailureEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onFailure(event);
        }
    }

    @SuppressWarnings("unused")
    public final void onEventMainThread(ProgressEvent event) {
        if (taskName().equals(event.getTaskName())) {
            onProgress(event);
        }
    }

    @Override
    public void onSuccess(SuccessEvent event) {}

    @Override
    public void onFailure(FailureEvent event) {}

    @Override
    public void onProgress(ProgressEvent event) {}
}
