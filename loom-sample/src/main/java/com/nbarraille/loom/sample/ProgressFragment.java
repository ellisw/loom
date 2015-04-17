package com.nbarraille.loom.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nbarraille.loom.Loom;
import com.nbarraille.loom.listeners.SimpleUiThreadListener;

/**
 * Fragment showing how to start a task and setup a listener that will get called when the task
 * finishes.
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class ProgressFragment extends Fragment {
    private ProgressBar mProgressBar;

    private SimpleUiThreadListener<TaskProgress.Success, TaskProgress.Failure, TaskProgress.Progress> mListener =
            new SimpleUiThreadListener<TaskProgress.Success, TaskProgress.Failure, TaskProgress.Progress>() {
                @Override
                public void onSuccess(TaskProgress.Success event) {
                    Log.i("FlySample", "Success Received for task Progress");
                    mProgressBar.setProgress(100);
                }

                @Override
                public void onFailure(TaskProgress.Failure event) {
                    Log.i("FlySample", "Failure Received for task Progress");
                    mProgressBar.setProgress(0);
                }

                @Override
                public void onProgress(TaskProgress.Progress event) {
                    Log.i("FlySample", "Progress Received for task Progress: " + event.getProgress());
                    mProgressBar.setProgress(event.getProgress());
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
}
