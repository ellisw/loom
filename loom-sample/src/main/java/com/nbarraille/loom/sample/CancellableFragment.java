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
import com.nbarraille.loom.listeners.LoomListener;
import com.nbarraille.loom.listeners.SimpleUiThreadListener;

/**
 * Fragment showing how to start a task and setup a listener that will update a progress bar when
 * the task progresses. The fragment just needs to get a hold to the ID of the task and it can
 * cancel it even after orientation change
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class CancellableFragment extends Fragment {
    private final static String SIS_KEY_TASK_ID = "task_id";
    private ProgressBar mProgressBar;
    private int mTaskId;

    private LoomListener mListener = new SimpleUiThreadListener<TaskCancellable.Success, TaskCancellable.Failure, TaskCancellable.Progress>() {
                @Override
                public void onSuccess(TaskCancellable.Success event) {
                    Log.i("FlySample", "Success Received for task Cancellable");
                    mProgressBar.setProgress(100);
                    mTaskId = -1;
                }

                @Override
                public void onFailure(TaskCancellable.Failure event) {
                    Log.i("FlySample", "Failure Received for task Cancellable");
                    mProgressBar.setProgress(0);
                    mTaskId = -1;
                }

                @Override
                public void onProgress(TaskCancellable.Progress event) {
                    Log.i("FlySample", "Progress Received for task Cancellable: " + event.getProgress());
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
                    Log.i("FlySample", "A Cancellable task is already running");
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

        Loom.registerListener(mListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Loom.unregisterListener(mListener);
    }
}
