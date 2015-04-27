package com.nbarraille.loom;


import android.support.annotation.Nullable;

import com.nbarraille.loom.events.FailureEvent;
import com.nbarraille.loom.events.ProgressEvent;
import com.nbarraille.loom.events.SuccessEvent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class LoomTest {
    private final static long TASK_DURATION = 300; // The duration of a fake task, in ms
    private final static long DURATION_BEFORE_CANCEL = 100; // The amount of time we should wait before cancelling a task to make sure it starts its execution
    private final static long TIMEOUT = 10; // Time to wait for the executor to finish, in seconds
    private TaskManager mTaskManager;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        mTaskManager = new TaskManager.Builder().setExecutor(Executors.newSingleThreadExecutor()).build();
    }

    /**
     * A utility method that shuts down the executor service and waits for all the tasks to complete
     * @throws InterruptedException
     */
    private void waitForIdle() throws InterruptedException {
        ExecutorService executor = ((ExecutorService) mTaskManager.getExecutor());
        executor.shutdown();
        executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void testSuccess() throws InterruptedException {
        GenericEventCatcher eventCatcher = new GenericEventCatcher("test1");
        mTaskManager.registerListener(eventCatcher);
        mTaskManager.execute(new Task() {
            @Override
            protected String name() {
                return "test1";
            }

            @Override
            protected void runTask() throws Exception {}
        });

        waitForIdle();
        mTaskManager.unregisterListener(eventCatcher);

        assertNotNull("The listener did not receive a success", eventCatcher.getReceivedSuccess());
        assertNull("The listener received a failure event", eventCatcher.getReceivedFailure());
        assertEquals("The listener received progress events", 0, eventCatcher.getReceivedProgresses().size());
    }

    @Test
    public void testFailure() throws InterruptedException {
        GenericEventCatcher eventCatcher = new GenericEventCatcher("test2");
        mTaskManager.registerListener(eventCatcher);
        mTaskManager.execute(new Task() {
            @Override
            protected String name() {
                return "test2";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }
        });

        waitForIdle();
        mTaskManager.unregisterListener(eventCatcher);

        assertNull("The listener received a success", eventCatcher.getReceivedSuccess());
        assertNotNull("The listener did not receive a failure event", eventCatcher.getReceivedFailure());
        assertEquals("The listener received progress events", 0, eventCatcher.getReceivedProgresses().size());
    }

    @Test
    public void testProgress() throws InterruptedException {
        GenericEventCatcher eventCatcher = new GenericEventCatcher("test2");
        mTaskManager.registerListener(eventCatcher);
        mTaskManager.execute(new Task() {
            @Override
            protected String name() {
                return "test2";
            }

            @Override
            protected void runTask() throws Exception {
                postProgress(1);
                postProgress(50);
            }
        });

        waitForIdle();
        mTaskManager.unregisterListener(eventCatcher);

        assertNotNull("The listener did not receive a success", eventCatcher.getReceivedSuccess());
        assertNull("The listener received a failure event", eventCatcher.getReceivedFailure());
        List<ProgressEvent> progresses = eventCatcher.getReceivedProgresses();
        assertEquals("The listener received the wrong number of progress events", 2, progresses.size());
        assertEquals("The listener received progress with a wrong value", 1, progresses.get(0).getProgress());
        assertEquals("The listener received progress with a wrong value", 50, progresses.get(1).getProgress());
    }

    @Test
    public void testProgressWithLargeValue() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {

            }
        };

        exception.expect(IllegalArgumentException.class);
        task.postProgress(150);
    }

    @Test
    public void testProgressWithNegativeValue() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {}
        };
        exception.expect(IllegalArgumentException.class);
        task.postProgress(-1);
    }

    @Test
    public void testCannotSendProgressAfterSuccess() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {}
        };

        mTaskManager.execute(task);
        waitForIdle();

        exception.expect(IllegalStateException.class);
        task.postProgress(10);
    }

    @Test
    public void testCannotSendProgressAfterFailure() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }
        };

        mTaskManager.execute(task);
        waitForIdle();

        exception.expect(IllegalStateException.class);
        task.postProgress(10);
    }

    @Test
    public void testTaskFlags() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void runTask() throws Exception {}
        };

        assertEquals("The task was marked as cancelled", false, task.isCancelled());
        assertEquals("The task was marked as finished", false, task.isFinished());

        mTaskManager.execute(task);
        waitForIdle();

        assertEquals("The task was marked as cancelled", false, task.isCancelled());
        assertEquals("The task was not marked as finished", true, task.isFinished());
    }

    @Test
    public void testQueuedCancelledTaskDoesNotRun() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void runTask() throws Exception {
                Assert.fail("Task ran");
            }
        };

        task.cancel();

        mTaskManager.execute(task);
        waitForIdle();

        assertEquals("The task was not marked as cancelled", true, task.isCancelled());
    }

    @Test
    public void testRunningCancelledTaskStops() throws Exception {
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void runTask() throws Exception {
                postProgress(1);
                Thread.sleep(TASK_DURATION);
                Assert.fail("Task did not stop");
            }
        };

        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        Thread.sleep(DURATION_BEFORE_CANCEL);
        task.cancel();
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertEquals("The task was not marked as cancelled", true, task.isCancelled());
        assertEquals("The task did not start", 1, catcher.getReceivedProgresses().size());
    }

    @Test
    public void testCannotCancelNonCancellableTask() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {}
        };

        exception.expect(IllegalStateException.class);
        task.cancel();
    }

    private class CustomSuccessEvent extends SuccessEvent {}
    private class CustomFailureEvent extends FailureEvent {}
    private class CustomProgressEvent extends ProgressEvent {
        public CustomProgressEvent(int progress) {
            super(progress);
        }
    }

    @Test
    public void testBuildCustomSuccessEvent() throws Exception {
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
            }

            @Nullable
            @Override
            protected SuccessEvent buildSuccessEvent() {
                return new CustomSuccessEvent();
            }
        };

        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertNotNull("The task did not succeed", catcher.getReceivedSuccess());
        assertEquals("The task did not send its custom success", CustomSuccessEvent.class, catcher.getReceivedSuccess().getClass());
    }

    @Test
    public void testBuildCustomFailureEvent() throws Exception {
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }

            @Nullable
            @Override
            protected FailureEvent buildFailureEvent() {
                return new CustomFailureEvent();
            }
        };

        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertNotNull("The task did not fail", catcher.getReceivedFailure());
        assertEquals("The task did not send its custom failure", CustomFailureEvent.class, catcher.getReceivedFailure().getClass());
    }

    @Test
    public void testBuildCustomProgressEvent() throws Exception {
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                postProgress(50);
            }

            @Nullable
            @Override
            protected ProgressEvent buildProgressEvent(int progress) {
                return new CustomProgressEvent(progress);
            }
        };

        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertEquals("The task did not receive progress", 1, catcher.getReceivedProgresses().size());
        assertEquals("The task did not send its custom progress", CustomProgressEvent.class, catcher.getReceivedProgresses().get(0).getClass());
    }

    @Test
    public void testOnSuccessCallback() throws Exception {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
            }

            @Override
            protected void onSuccess() {
                wasCalled.set(true);
            }
        };

        mTaskManager.execute(task);
        waitForIdle();

        assertEquals("onSuccess() was not called", true, wasCalled.get());
    }

    @Test
    public void testOnFailureCallback() throws Exception {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }

            @Override
            protected void onFailure(Exception e) {
                wasCalled.set(true);
            }
        };

        mTaskManager.execute(task);
        waitForIdle();

        assertEquals("onFailure() was not called", true, wasCalled.get());
    }

    @Test
    public void testOnCancelledCallback() throws Exception {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
                Assert.fail("Task did not stop");
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void onCancelled() {
                wasCalled.set(true);
            }
        };

        mTaskManager.execute(task);
        Thread.sleep(DURATION_BEFORE_CANCEL);
        task.cancel();
        waitForIdle();

        assertEquals("onCancelled() was not called", true, wasCalled.get());
    }

    @Test
    public void testOnCancelledBeforeRunning() throws Exception {
        // onCancelled should not be called if the task has not started running yet
        final AtomicBoolean wasCalled = new AtomicBoolean(false);
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Assert.fail("Task ran");
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void onCancelled() {
                wasCalled.set(true);
            }
        };

        task.cancel();
        mTaskManager.execute(task);
        waitForIdle();

        assertEquals("onCancelled() was called", false, wasCalled.get());
    }

    @Test
    public void testErrorWhileOnSuccess() throws Exception {
        // An error while Task.onSuccess() should not prevent the success event to be sent
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {}

            @Override
            protected void onSuccess() {
                throw new RuntimeException("Something went wrong");
            }
        };
        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertNotNull("Success was not received", catcher.getReceivedSuccess());
    }

    @Test
    public void testErrorWhileOnFailure() throws Exception {
        // An error while Task.onFailure() should not prevent the failure event to be sent
        EventCatcher catcher = new GenericEventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("TaskFailed");
            }

            @Override
            protected void onFailure(Exception e) {
                throw new RuntimeException("Something went wrong");
            }
        };
        mTaskManager.registerListener(catcher);
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.unregisterListener(catcher);

        assertNotNull("Failure was not received", catcher.getReceivedFailure());
    }

    @Test
    public void testErrorWhileOnCancelled() throws Exception {
        // An error while Task.onCancelled() should not crash
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
                Assert.fail("Task did not stop");
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }

            @Override
            protected void onCancelled() {
                throw new RuntimeException("Something went wrong");
            }
        };

        mTaskManager.execute(task);
        Thread.sleep(DURATION_BEFORE_CANCEL);
        task.cancel();
        waitForIdle();
    }

    @Test
    public void testRegisterNullListener() throws Exception {
        exception.expect(NullPointerException.class);
        //noinspection ConstantConditions
        mTaskManager.registerListener(null);
    }

    @Test
    public void testUnregisterNullListener() throws Exception {
        exception.expect(NullPointerException.class);
        //noinspection ConstantConditions
        mTaskManager.unregisterListener(null);
    }

    @Test
    public void testRunNullTask() throws Exception {
        exception.expect(NullPointerException.class);
        //noinspection ConstantConditions
        mTaskManager.execute(null);
    }

    @Test
    public void testSingleThreadedExecution() throws Exception {
        // On a single threaded executor, the second task should not start until the first one finishes
        // EventCatcher's implementation will also make sure that listeners won't receive events for
        // other tasks
        EventCatcher catcher1 = new EventCatcher("task1");
        EventCatcher catcher2 = new EventCatcher("task2");
        final Task task1 = new Task() {
            @Override
            protected String name() {
                return "task1";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };

        Task task2 = new Task() {
            @Override
            protected String name() {
                return "task2";
            }

            @Override
            protected void runTask() throws Exception {
                if (!task1.isFinished()) {
                    Assert.fail("Task2 started before Task1 finished");
                }
                Thread.sleep(TASK_DURATION);
            }
        };
        mTaskManager = new TaskManager.Builder().setExecutor(Executors.newSingleThreadExecutor()).build();
        mTaskManager.registerListener(catcher1);
        mTaskManager.registerListener(catcher2);
        mTaskManager.execute(task1);
        mTaskManager.execute(task2);
        waitForIdle();
        mTaskManager.unregisterListener(catcher1);
        mTaskManager.unregisterListener(catcher2);

        assertNotNull("Success was not received", catcher1.getReceivedSuccess());
        assertNotNull("Success was not received", catcher2.getReceivedSuccess());
    }

    @Test
    public void testMultiThreadedExecution() throws Exception {
        // On a multi threaded executor, the second task should start before the first one finishes
        // EventCatcher's implementation will also make sure that listeners won't receive events for
        // other tasks
        EventCatcher catcher1 = new EventCatcher("task1");
        EventCatcher catcher2 = new EventCatcher("task2");
        final Task task1 = new Task() {
            @Override
            protected String name() {
                return "task1";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };

        Task task2 = new Task() {
            @Override
            protected String name() {
                return "task2";
            }

            @Override
            protected void runTask() throws Exception {
                if (task1.isFinished()) {
                    Assert.fail("Task2 started after Task1 finished");
                }
                Thread.sleep(TASK_DURATION);
            }
        };
        mTaskManager = new TaskManager.Builder().setExecutor(Executors.newFixedThreadPool(2)).build();

        mTaskManager.registerListener(catcher1);
        mTaskManager.registerListener(catcher2);
        mTaskManager.execute(task1);
        mTaskManager.execute(task2);
        waitForIdle();
        mTaskManager.unregisterListener(catcher1);
        mTaskManager.unregisterListener(catcher2);

        assertNotNull("Success was not received", catcher1.getReceivedSuccess());
        assertNotNull("Success was not received", catcher2.getReceivedSuccess());
    }

    @Test
    public void testMultipleTaskManagers() throws Exception {
        // On a multi threaded executor, the second task should start before the first one finishes
        // EventCatcher's implementation will also make sure that listeners won't receive events for
        // other tasks
        EventCatcher catcher1 = new EventCatcher("task");
        EventCatcher catcher2 = new EventCatcher("task");
        final Task task1 = new Task() {
            @Override
            protected String name() {
                return "task";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };

        Task task2 = new Task() {
            @Override
            protected String name() {
                return "task";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };

        TaskManager manager1 = new TaskManager.Builder().build();
        TaskManager manager2 = new TaskManager.Builder().build();
        manager1.registerListener(catcher1);
        manager2.registerListener(catcher2);
        manager1.execute(task1);
        manager2.execute(task2);
        ((ExecutorService) manager1.getExecutor()).shutdown();
        ((ExecutorService) manager1.getExecutor()).awaitTermination(TIMEOUT, TimeUnit.SECONDS);
        ((ExecutorService) manager2.getExecutor()).shutdown();
        ((ExecutorService) manager2.getExecutor()).awaitTermination(TIMEOUT, TimeUnit.SECONDS);
        manager1.unregisterListener(catcher1);
        manager2.unregisterListener(catcher2);

        assertNotNull("Success was not received", catcher1.getReceivedSuccess());
        assertNotNull("Success was not received", catcher2.getReceivedSuccess());
    }

    @Test
    public void testStickyRegisterListenerGetsSuccess() throws Exception {
        EventCatcher catcher = new EventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
            }
        };
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.registerListener(catcher, task.getId());
        Thread.sleep(DURATION_BEFORE_CANCEL);
        assertNotNull(catcher.getReceivedSuccess());
    }

    @Test
    public void testStickyRegisterListenerGetsFailure() throws Exception {
        EventCatcher catcher = new EventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }
        };
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.registerListener(catcher, task.getId());
        Thread.sleep(DURATION_BEFORE_CANCEL);
        assertNotNull(catcher.getReceivedFailure());
    }

    @Test
    public void testNonStickyRegisterListenerDoesNotGetSuccess() throws Exception {
        EventCatcher catcher = new EventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
            }
        };
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.registerListener(catcher);
        Thread.sleep(DURATION_BEFORE_CANCEL);
        assertNull(catcher.getReceivedSuccess());
    }

    @Test
    public void testNonStickyRegisterListenerDoesNotGetFailure() throws Exception {
        EventCatcher catcher = new EventCatcher("test");
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                throw new RuntimeException("Task failed");
            }
        };
        mTaskManager.execute(task);
        waitForIdle();
        mTaskManager.registerListener(catcher);
        Thread.sleep(DURATION_BEFORE_CANCEL);
        assertNull(catcher.getReceivedFailure());
    }

    @Test
    public void testStickyRegisterListenerWithWrongId() throws Exception {
        EventCatcher catcher = new EventCatcher("test1");
        Task task1 = new Task() {
            @Override
            protected String name() {
                return "test1";
            }

            @Override
            protected void runTask() throws Exception {
            }
        };
        Task task2 = new Task() {
            @Override
            protected String name() {
                return "test2";
            }

            @Override
            protected void runTask() throws Exception {
            }
        };
        mTaskManager.execute(task1);
        waitForIdle();
        mTaskManager.registerListener(catcher, task2.getId());
        Thread.sleep(DURATION_BEFORE_CANCEL);
        assertNull(catcher.getReceivedSuccess());
    }

    @Test
    public void testGetTaskStatus() throws Exception {
        Task task1 = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };
        Task task2 = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
                throw new RuntimeException("Task failed");
            }
        };

        TaskManager tm = new TaskManager.Builder().setExecutor(Executors.newSingleThreadExecutor()).build();

        assertNull(mTaskManager.getTaskStatus(task1.getId()));
        assertNull(mTaskManager.getTaskStatus(task2.getId()));
        tm.execute(task1);
        tm.execute(task2);
        Thread.sleep(DURATION_BEFORE_CANCEL);

        TaskStatus status1 = tm.getTaskStatus(task1.getId());
        assertEquals(TaskStatus.STARTED, status1.getStatus());
        assertTrue(status1.isStarted());
        assertFalse(status1.isPending());
        assertFalse(status1.isFinished());
        assertFalse(status1.isCancelled());

        TaskStatus status2 = tm.getTaskStatus(task2.getId());
        assertEquals(TaskStatus.PENDING, status2.getStatus());
        assertTrue(status2.isPending());
        assertFalse(status2.isStarted());
        assertFalse(status2.isFinished());
        assertFalse(status2.isCancelled());

        ((ExecutorService) tm.getExecutor()).shutdown();
        ((ExecutorService) tm.getExecutor()).awaitTermination(TIMEOUT, TimeUnit.SECONDS);

        status1 = tm.getTaskStatus(task1.getId());
        assertEquals(TaskStatus.FINISHED, status1.getStatus());
        assertTrue(status1.isFinished());
        assertFalse(status1.isStarted());
        assertFalse(status1.isPending());
        assertFalse(status1.isCancelled());
        assertNotNull(status1.getSuccessEvent());
        assertNull(status1.getFailureEvent());

        status2 = tm.getTaskStatus(task2.getId());
        assertEquals(TaskStatus.FINISHED, status2.getStatus());
        assertTrue(status2.isFinished());
        assertFalse(status2.isStarted());
        assertFalse(status2.isPending());
        assertFalse(status2.isCancelled());
        assertNull(status2.getSuccessEvent());
        assertNotNull(status2.getFailureEvent());
    }

    @Test
    public void testCancelledTaskStatus() throws Exception {
        Task task = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }

            @Override
            protected boolean isCancellable() {
                return true;
            }
        };

        mTaskManager.execute(task);
        mTaskManager.cancelTask(task.getId());

        TaskStatus status = mTaskManager.getTaskStatus(task.getId());
        assertEquals(TaskStatus.CANCELLED, status.getStatus());
        assertFalse(status.isFinished());
        assertFalse(status.isStarted());
        assertFalse(status.isPending());
        assertTrue(status.isCancelled());
        assertNull(status.getSuccessEvent());
        assertNull(status.getFailureEvent());
    }

    @Test
    public void testBacklogSize() throws Exception {
        TaskManager tm = new TaskManager.Builder().setMaxBacklogSize(1).build();
        Task task1 = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };
        Task task2 = new Task() {
            @Override
            protected String name() {
                return "test";
            }

            @Override
            protected void runTask() throws Exception {
                Thread.sleep(TASK_DURATION);
            }
        };

        tm.execute(task1);
        tm.execute(task2);

        assertNull(tm.getTaskStatus(task1.getId()));
        assertNotNull(tm.getTaskStatus(task2.getId()));
    }

    @Test
    public void testInvalidBacklogSize() throws Exception {
        exception.expect(IllegalArgumentException.class);
        new TaskManager.Builder().setMaxBacklogSize(0).build();

        exception.expect(IllegalArgumentException.class);
        new TaskManager.Builder().setMaxBacklogSize(-10).build();
    }
}
