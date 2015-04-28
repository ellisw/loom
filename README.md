Loom
====

Loom is a simple and powerful library for running and managing tasks in background threads for Android.
It executes tasks in the background and sends messages on an Event Bus to notify listeners of the progress of the task.

Context
-------
There are a lot of ways to chose from to run background tasks in Android: AsyncTasks, Threads, IntentServices, Loaders...
Chosing from them can be very complex, and seasoned Android developers know how hard it can be to implement threading that works in every situation.
Loom believes that you should be able to simply start background tasks from any part of your app, and be able to get notifications for those tasks anywhere, simply, and without fear or leaking resources.

Features
--------
  - `Tasks` are completely separated from `Activities`/`Services` lifecycle and will persist if the user goes away. This means that orientation changes are supported without effort. This also means that you don't need to retain instances of your activity/fragments and they won't leak.
  - The Tasks can notify `LoomListeners` when a task succeeds, fails, or when a `Task` progress has changed. You can register as many Listeners as you want.
  - You can choose explicitly whether your listeners callbacks should be executed on the UI thread or in the background.
  - The Events sent back to the listener can contain any data you want, they don't need to be `Serializable`.
  - Tasks can be cancelled at any time from anywhere, without having to keep references of them.
  - Tasks run on a standard Java `Executor`. You can use the default one we provide, or customize your own.
  - For advance threading needs, you can create as many `TaskManagers` as you'd like, so that different tasks can run on different `Executors`.
  - The `EventBus` the Events are sent on is fully customizable too.
  - The TaskManager keeps track of a `Task` backlog, so that you can query status of recent Tasks.
  - (COMING SOON) This is fully testable and integrated with `Espresso`

Usage
-----

Execute a task in the background
--------------------------------
First, we need to create a `Task` to execute in the background. It is recommended to keep your tasks classes top-level or static inner classes. You should avoid non-static inner classes or anonymous classes, as this will leak the outer class (often a `Fragment` or `Activity`) the same way it happens with `AsyncTask`.
```
public MyTask extends Task {
    @Override
    protected void runTask() throws Exception {
        // The actual code that is ran in the background when the task is executed
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
        }
    }

    @Override
    protected String name() {
        // The name of the task, used to match events to the appropriate listeners
        return "MyTask";
    }
}
```
You can now run this task:
```
Loom.execute(new MyTask());
```

Listen for task progress
------------------------
Loom provides `LoomListener` objects that can be registered to get notified about 3 types of status change: Success, Failure and Progress change.
By default, the task will send a `SuccessEvent` and `FailureEvent` on the bus to notify for success and failure. It is up to the task itself to call `postProgress()` from its `runTask()` method to notify of its progress.

Simply create a `LoomListener` and register/unregister it to listen for events:
You should inherit from `GenericUiThreadListener` if you want your callbacks to be ran in the UI thread, or from `GenericBackgroundListener` for the callbacks to be ran in a background thread.
```
LoomListener listener = new GenericUiThreadListener {
    @Override
    public String taskName() {
        // The name of the task we're listening to, this must match Task.name()
        return "MyTask";
    }

    @Override
    public void onSuccess(SuccessEvent event) {
        // Success received
    }

    @Override
    public void onFailure(FailureEvent event) {
      // Failure received
    }

    @Override
    public void MyTask(ProgressEvent event) {
        // Progress received
        mProgressBar.setProgress(event.getProgress());
    }
};
```

```
public class MyActivity extends Activity {
    ...
    @Override
    public void onResume() {
        super.onResume();

        Loom.registerListener(listener);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        Loom.unregisterListener(listener);
    }
}
```

Make sure you don't miss any events
-----------------------------------
A common scenario where you can miss Success/Failure events, is when an `Activity` that contains a `LoomListener` is re-created due to a configuration change, and the `Task` happens to finish right between the time the old `Activity` unregisters its Listener, and the time the new `Activity` registers its Listener.
To prevent this, you can pass a task ID when registering the new Listener, and its callback will immediately be executed if the `Task` has already finished:

```
public class MyActivity extends Activity {
    private int mTaskId;
    ...
    @Override
    public void onResume() {
        super.onResume();

        // Calling registerListener with task ID instead of Loom.registerListener(listener)
        Loom.registerListener(listener, mTaskId);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        Loom.unregisterListener(listener);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaverInstanceState(outState);
        outState.putInt("TASK_ID", mTaskId);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mTaskId = savedInstanceState.getInt("TASK_ID");
        }
    }
}
```

Custom events
-------------
The default `SuccessEvent`, `FailureEvent` and `ProgressEvent` don't pass much information back to the listeners, but you can add any data to it by subclassing those events:

1. Create a custom event type. They can contain any kind of data, not only `Serializable`/`Parcelable` objects.
  ```
class NumberGeneratorSuccess extends SuccessEvent {
    final int number;

    public NumberGeneratorSuccess(int number) {
        this.number = number;
    }
}
  ```

2. Override `buildSuccessEvent()`, `buildErrorEvent()` and/or `buildProgressEvent()` to return and initialize your custom event types
  ```
/**
 * Simple task that reports its success and failure to its listeners but no progress
 */
class NumberGeneratorTask extends Task {
    private int mGeneratedNumber;

    @Override
    protected void runTask() throws Exception {
        Thread.sleep(2000);
        mGeneratedNumber = new Random().nextInt();
    }

    @Override
    public String name() {
        return "NumberGenerator"
    }

    @Nullable
    @Override
    protected SuccessEvent buildSuccessEvent() {
        return new NumberGeneratorSuccess(mGeneratedNumber);
    }
}
  ```

3. Create a `LoomListener` with the matching event types
  ```
LoomListener mListener = new UiThreadListener<NumberGeneratorSuccess, FailureEvent, ProgressEvent>() {
    @NonNull
    @Override
    public String taskName() {
        return "NumberGenerator";
    }

    @Override
    public void onSuccess(NumberGeneratorSuccess event) {
        Log.i("LoomSample", "Generated number: " + event.number);
    }
};
  ```

Cancel a task
-------------
`Loom.execute(task)` returns the ID of the task it scheduled. You can cancel this task by calling `Loom.cancelTask(id)`. This is done so that you don't have to keep a reference to the `Task` object to cancel it, but only to its ID.
By default, Tasks are not cancellable so that they won't be left in an indeterminate state. If you want your `Task` to be cancellable, you need to override its `isCancellable()` method to return `true`.
If a `Task` is cancelled before it started executing, it won't be executed. If a `Task` is cancelled during its execution, an interruption is send to the `Thread` the task is running on.
Most of the long running methods (such as `Thread.sleep()`, most networking libraries blocking calls, ...) do check `Thread.isInterrupted()` while they are running, but if you're doing something that doesn't handle interruptions, you will have to manually check `isCancelled()` and throw an `InterruptedException` if necessary.
`Task` has a `onCancelled()` callback that is executed whenever the `Task` is cancelled (only if the execution of that Task has started.
```
public class MyTask extends Task {
    ...
    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            myCustomOperation();
            if (isCancelled()) {
              throw new InterruptedException("The task has been cancelled");
            }
        }
    }
    
    @Override
    protected boolean isCancellable() {
        return true;
    }

    @Override
    protected void onCancelled() {
        // clean up
    }
```

Customize the Threading
-----------------------
The default instance of `Loom` runs the task on the default `AsyncTask.THREAD_POOL_EXECUTOR` executor. If you want to do something more customized or use different Executors for different tasks, you can create your own `TaskManager` instance with a custom `Executor`.
```
Executor executor = ExecutorService.newFixedThreadPool(5);
TaskManager taskManager = new TaskManager.Builder().setExecutor(executor).build();
taskManager.execute(new MyTask());
```
Or you can just customize the default `Loom`
```
Executor executor = ExecutorService.newFixedThreadPool(5);
Loom.configureDefault(new LoomConfig().setExecutor(executor));
Loom.execute(new MyTask());
```

You can also customize the message bus in the same way
