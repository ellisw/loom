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

import android.support.annotation.NonNull;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

/**
 * A LoomListener that contains a set of callbacks being called at various points during the task
 * execution.
 * A Listener needs to be registered with the TaskManager for its callbacks to be called.
 */
public interface LoomListener<Success, Failure, Progress> {
    /**
     * Callback getting executed when the task finishes successfully
     * @param event the success event
     */
    void onSuccess(Success event);

    /**
     * Callback getting executed when the task fails
     * @param event the failure event
     */
    void onFailure(Failure event);

    /**
     * Callback getting executed when the progress of the task changes
     * Fly does not send any progress by default, it is up to individual task to report their
     * progress change
     * @param event the progress event
     */
    void onProgress(Progress event);

    /**
     * The name of the task we're monitoring
     * @return the name of the task
     */
    @NonNull String taskName();
}
