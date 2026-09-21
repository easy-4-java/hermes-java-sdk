package io.github.easy4j.hermes.api.sse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderedEventDispatcherTest {

    @Test
    void dispatchesInOrderAndReportsQueuedTasks() throws Exception {
        OrderedEventDispatcher dispatcher = new OrderedEventDispatcher("run:test", 4);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
        try {
            dispatcher.dispatch(() -> {
                firstStarted.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                order.add(1);
                completed.countDown();
            });
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

            dispatcher.dispatch(() -> {
                order.add(2);
                completed.countDown();
            });

            assertEquals(1, dispatcher.queuedTaskCount());
            releaseFirst.countDown();
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            assertEquals(java.util.Arrays.asList(1, 2), order);
        } finally {
            releaseFirst.countDown();
            dispatcher.close();
        }
    }

    @Test
    void rejectsWhenBoundedQueueIsFull() throws Exception {
        OrderedEventDispatcher dispatcher = new OrderedEventDispatcher("overflow", 1);
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        try {
            dispatcher.dispatch(() -> {
                workerStarted.countDown();
                try {
                    releaseWorker.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(workerStarted.await(2, TimeUnit.SECONDS));
            dispatcher.dispatch(() -> { });

            assertThrows(SseQueueOverflowException.class,
                    () -> dispatcher.dispatch(() -> { }));
        } finally {
            releaseWorker.countDown();
            dispatcher.close();
        }
    }

    @Test
    void closeIsIdempotentAndRejectsNewWork() {
        OrderedEventDispatcher dispatcher = new OrderedEventDispatcher(null, 1);
        dispatcher.close();
        dispatcher.close();

        assertThrows(IllegalStateException.class,
                () -> dispatcher.dispatch(() -> { }));
        assertThrows(NullPointerException.class,
                () -> dispatcher.dispatch(null));
        assertEquals(0, dispatcher.queuedTaskCount());
    }
}
