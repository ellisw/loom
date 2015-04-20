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
 * Fragment showing how to start a task and setup a listener that will get called when the task
 * finishes.
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class ProgressFragment extends Fragment {
    private final static String TASK_NAME = "NoProgressTask";
    private ProgressBar mProgressBar;

    private LoomListener mListener = new GenericUiThreadListener() {
        @Override
        public void onSuccess(SuccessEvent event) {
            Log.i("FlySample", "Success Received for task Progress");
            mProgressBar.setProgress(100);
        }

        @Override
        public void onFailure(FailureEvent event) {
            Log.i("FlySample", "Failure Received for task Progress");
            mProgressBar.setProgress(0);
        }

        @Override
        public void onProgress(ProgressEvent event) {
            Log.i("FlySample", "Progress Received for task Progress: " + event.getProgress());
            mProgressBar.setProgress(event.getProgress());
        }

        @NonNull
        @Override
        public String taskName() {
            return TASK_NAME;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.task_fragment, container, false);
        ((TextView) view.findViewById(R.id.title)).setText("Task with progress");
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        view.findViewById(R.id.cancel_button).setVisibility(View.GONE);

        view.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Loom.execute(new TaskProgress());
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

    /**
     * A simple task that reports it's success, failure, and progress to listeners
     */
    public static class TaskProgress extends Task {
        @Override
        protected String name() {
            return TASK_NAME;
        }

        @Override
        protected void runTask() throws Exception {
            for (int i = 0; i < 100; i++) {
                Log.i("FlySample", name() + " at " + i);
                postProgress(i);
                Thread.sleep(100);
            }
        }
    }
}
