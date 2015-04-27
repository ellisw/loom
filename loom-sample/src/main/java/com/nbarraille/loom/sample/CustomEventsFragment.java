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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nbarraille.loom.Loom;
import com.nbarraille.loom.Task;
import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;
import com.nbarraille.loom.listeners.LoomListener;
import com.nbarraille.loom.listeners.UiThreadListener;

import java.util.Random;

/**
 * Fragment showing how to start a task that will send events containing custom info to its listeners
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class CustomEventsFragment extends Fragment {
    private final static String TASK_NAME = "NumberGeneratorTask";

    private LoomListener mListener = new UiThreadListener<NumberGeneratorSuccess, FailureEvent, ProgressEvent>() {
        @NonNull
        @Override
        public String taskName() {
            return TASK_NAME;
        }

        @Override
        public void onSuccess(NumberGeneratorSuccess event) {
            Log.i("LoomSample", "Generated number: " + event.number);
        }

        @Override
        public void onFailure(FailureEvent event) {
            Log.i("LoomSample", "Failure Received for task NoProgress");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.task_fragment, container, false);
        ((TextView) view.findViewById(R.id.title)).setText("Task without progress");
        view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        view.findViewById(R.id.cancel_button).setVisibility(View.GONE);

        view.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Loom.execute(new NumberGeneratorTask());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Loom.registerListener(mListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Loom.unregisterListener(mListener);
    }

    private static class NumberGeneratorSuccess extends SuccessEvent {
        final int number;

        public NumberGeneratorSuccess(int number) {
            this.number = number;
        }
    }

    /**
     * Simple task that reports its success and failure to its listeners but no progress
     */
    public static class NumberGeneratorTask extends Task {
        private int mGeneratedNumber;

        @Override
        protected void runTask() throws Exception {
            Thread.sleep(2000);
            mGeneratedNumber = new Random().nextInt();
        }

        @Override
        protected String name() {
            return TASK_NAME;
        }

        @Nullable
        @Override
        protected SuccessEvent buildSuccessEvent() {
            return new NumberGeneratorSuccess(mGeneratedNumber);
        }
    }
}
