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
package com.nbarraille.loom.events;

/**
 * The base class for a Progress event
 */
public class ProgressEvent extends Event {
    private final int mProgress;

    public ProgressEvent(int progress) {
        mProgress = progress;
    }

    @SuppressWarnings("unused")
    public int getProgress() {
        return mProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ProgressEvent) {
            return mProgress == ((ProgressEvent) o).getProgress();
        } else {
            return false;
        }
    }
}
