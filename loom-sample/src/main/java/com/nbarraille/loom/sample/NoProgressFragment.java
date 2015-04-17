package com.nbarraille.loom.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nbarraille.loom.Loom;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.listeners.SimpleUiThreadListener;

/**
 * Fragment showing how to start a task and setup a listener that will get called when the task
 * finishes.
 * This will work perfectly fine when the fragment is recreated and there is no need to retain the
 * fragment!
 */
public class NoProgressFragment extends Fragment {
    private SimpleUiThreadListener<TaskNoProgress.Success, TaskNoProgress.Failure, ProgressEvent> mListener =
            new SimpleUiThreadListener<TaskNoProgress.Success, TaskNoProgress.Failure, ProgressEvent>() {
                @Override
                public void onSuccess(TaskNoProgress.Success event) {
                    Log.i("FlySample", "Success Received for task NoProgress");
                }

                @Override
                public void onFailure(TaskNoProgress.Failure event) {
                    Log.i("FlySample", "Failure Received for task NoProgress");
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
                Loom.execute(new TaskNoProgress());
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
