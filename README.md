Loom
====

Loom is a simple and powerful library for running and manging tasks in background threads for Android.
It executes tasks in the background and uses an Event Bus to notify listeners of the progress of the task.

Context
-------
There are a lot of ways to chose from to run background tasks in Android: AsyncTasks, Threads, IntentServices, Loaders...
Chosing from them can be very complex, and seasoned Android developers know how hard it can be to implement threading that works in every situation.
Loom believes that you should be able to simply start background tasks from any part of your app, and be able to get notifications for those tasks anywhere.


Features
--------
  - Background tasks are completely separated from activities/services lifecycle and will persist if the user goes away. This means that orientation changes are supported effortlessly. This also means that you don't need to retain instances of your activity/fragments and they won't leak. 
  - Background tasks can be cancelled at any time
  - Listeners can be notified when a task succeeds, fails, or when a task progress has changed
  - The bus is fully customizable
  - It uses the default AsyncTask thread pool by default, but the Executor is fully configurable
  
Usage
-----

Execute a task in the background
--------------------------------
First, we need to create a `Task` to execute in the Background. It is recommended to put your tasks classes in new file or static inner classes. You should avoid non-static inner classes, as this will leak the outer class (often a `Fragment` or `Activity`) the same way it happens with `AsyncTask`
```
public MyTask extends Task {
    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
        }
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
To create a Listener, you need to create subclasses of `SuccessEvent`, `FailureEvent`, and `ProgressEvent` for each event you're interested in receiving.

Modify your `Task` to send events we're interested in:
 - Create subclasses of `SuccessEvent`, `FailureEvent`, and `ProgressEvent` for each event you're interested in receiving
 - Override the `buildSuccessEvent()`, `buildFailureEvent()` and/or `buildProgressEvent()`
 - Success and Failure will be sent automatically when the `Task` finishes or fail, but it's up to your `Task` to report its own Progress change by calling `postProgress(progress)`
```
public class MyTask extends Task<MyTask.Success, MyTask.Failure, MyTask.Progress> {
    public static class Success extends SuccessEvent {
        // Those objects are POJO and you can add any data you want to them
    }
    public static class Failure extends FailureEvent {}
    public static class Progress extends ProgressEvent {
        public Progress(int progress) {
            super(progress);
        }
    }

    @Override
    protected void runTask() throws Exception {
        for (int i = 0; i < 100; i++) {
            postProgress(i);
            Thread.sleep(100);
        }
    }

    @Nullable
    @Override
    protected Success buildSuccessEvent() {
        return new Success();
    }

    @Nullable
    @Override
    protected Failure buildFailureEvent() {
        return new Failure();
    }

    @Nullable
    @Override
    protected Progress buildProgressEvent(@SuppressWarnings("UnusedParameters") int progress) {
        return new Progress(progress);
    }
}
```

Create a `Listener`:
```
LoomListener listener = new SimpleUiThreadListener<MyTask.Success, MyTask.Failure, MyTask.Progress> {
    @Override
    public void onSuccess(MyTask.Success event) {
        // Success received
    }

    @Override
    public void onFailure(MyTask.Failure event) {
      // Failure receive
    }

    @Override
    public void MyTask(TaskCancellable.Progress event) {
        // Progress received
        mProgressBar.setProgress(event.getProgress());
    }
};
```
Register (and unregister) the listener on Loom
-----------------------------
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

Cancel a task
-------------
`Loom.execute(task)` returns the ID of the task it scheduled. You can cancel this task by calling `Loom.cancelTask(id)`. This is done so that you don't have to keep a reference to the `Task` object to cancel it, but only to its ID.
By default, Tasks are not cancellable so that they won't be left in an indeterminate state. If you want your `Task` to be cancellable, you need to override its `isCancellable()` method.
If a `Task` is cancelled before it started executing, it won't be executed. If a `Task` is cancelled during its execution, an interruption is send to the `Thread` the task is running on.
Most of the long running methods (such as `Thread.sleep()`, most networking libraries blocking calls, ...) do check `Thread.isInterrupted() while they are running, but if you're doing something that doesn't handle interruptions, you will have to manually check `isCancelled()` and throw an `InterruptedException` if necessary.
`Task` has a `onCancelled()` callback that is executed whenever the `Task` 
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
