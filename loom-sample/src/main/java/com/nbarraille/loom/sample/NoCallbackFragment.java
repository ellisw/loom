package com.nbarraille.loom.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nbarraille.loom.Loom;

/**
 * Fragment showing how to start a task without being interested in any callback.
 * This task will stay alive even if the activity or fragment is stopped or destroyed and won't leak
 * the activity/fragment
 */
public class NoCallbackFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.task_fragment, container, false);
        ((TextView) view.findViewById(R.id.title)).setText("Task without callback");
        view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        view.findViewById(R.id.cancel_button).setVisibility(View.GONE);

        view.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Loom.execute(new TaskNoCallback());
            }
        });

        return view;
    }
}
