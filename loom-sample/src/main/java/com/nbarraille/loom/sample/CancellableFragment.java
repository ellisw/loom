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
package com.nbarraille.loom.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nbarraille.loom.Loom;
import com.nbarraille.loom.Task;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;
import com.nbarraille.loom.listeners.GenericUiThreadListener;
import com.nbarraille.loom.listeners.LoomListener;

/**
 * Fragment showing how to start a task and setup a listener that will update a progress bar when
 * the task progresses. The fragment just needs to get a hold to the ID of the task and it can
 * cancel it even after orientation change
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class CancellableFragment extends Fragment {
    private final static String TASK_NAME = "CancellableTask";
    private final static String SIS_KEY_TASK_ID = "task_id";
    private ProgressBar mProgressBar;
    private int mTaskId;

    private LoomListener mListener = new GenericUiThreadListener() {
        @NonNull
        @Override
        public String taskName() {
            return TASK_NAME;
        }

        @Override
        public void onSuccess(SuccessEvent event) {
            Log.i("LoomSample", "Success Received for task Cancellable");
            mProgressBar.setProgress(100);
            mTaskId = -1;
        }

        @Override
        public void onFailure(FailureEvent event) {
            Log.i("LoomSample", "Failure Received for task Cancellable");
            mProgressBar.setProgress(0);
            mTaskId = -1;
        }

        @Override
        public void onProgress(ProgressEvent event) {
            Log.i("LoomSample", "Progress Received for task Cancellable: " + event.getProgress());
            mProgressBar.setProgress(event.getProgress());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.task_fragment, container, false);
        ((TextView) view.findViewById(R.id.title)).setText("Cancellable Task");
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        mTaskId = -1;
        if (savedInstanceState != null) {
            mTaskId = savedInstanceState.getInt(SIS_KEY_TASK_ID);
        }

        view.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If your task is only being fired from one place, you can keep a reference to the
                // taskId to enforce that a task is not scheduled multiple times
                if (mTaskId == -1) {
                    mTaskId = Loom.execute(new TaskCancellable());
                } else {
                    Log.i("LoomSample", "A Cancellable task is already running");
                }
            }
        });
        view.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTaskId != -1) {
                    Loom.cancelTask(mTaskId);
                    mTaskId = -1;
                }
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // For cancellation to be able to work after the fragment gets recreated, no need to hang on
        // to the task instance, you can just pass around the task ID
        outState.putInt(SIS_KEY_TASK_ID, mTaskId);
    }

    @Override
    public void onResume() {
        super.onResume();

        Loom.registerListener(mListener, mTaskId);
    }

    @Override
    public void onPause() {
        super.onPause();

        Loom.unregisterListener(mListener);
    }

    /**
     * A simple task that reports it's success, failure, and progress to listeners and is cancellable
     */
    public static class TaskCancellable extends Task {
        @Override
        protected String name() {
            return TASK_NAME;
        }

        @Override
        protected void runTask() throws Exception {
            for (int i = 0; i < 100; i++) {
                Log.i("LoomSample", "Task Cancellable at " + i);
                postProgress(i);
                Thread.sleep(100);
            }
        }

        @Override
        protected boolean isCancellable() {
            return true;
        }

        @Override
        protected void onCancelled() {
            Log.i("LoomSample", "Task Cancellable cancelled, cleaning up...");
        }
    }
}
