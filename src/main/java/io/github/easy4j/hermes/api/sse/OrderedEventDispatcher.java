package io.github.easy4j.hermes.api.sse;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes callbacks for one SSE subscription in arrival order without occupying the network callback thread.
 *
 * <p>Each dispatcher has one daemon worker and a bounded queue. Queue saturation is explicit:
 * callers receive {@link SseQueueOverflowException} rather than silently losing an event.</p>
 *
 * @since 1.0.0
 */
public final class OrderedEventDispatcher implements AutoCloseable {

    private final ThreadPoolExecutor executor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public OrderedEventDispatcher(String label, int queueCapacity) {
        final String threadName = "hermes-sse-events-" + sanitize(label);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        };
        this.executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(Math.max(1, queueCapacity)),
                factory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public void dispatch(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (closed.get()) {
            throw new IllegalStateException("SSE event dispatcher is closed");
        }
        try {
            executor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            throw new SseQueueOverflowException(
                    "Hermes SSE callback queue is full for this subscription");
        }
    }

    public int queuedTaskCount() {
        return executor.getQueue().size();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
            executor.getQueue().clear();
        }
    }

    private static String sanitize(String label) {
        if (label == null || label.isEmpty()) {
            return "stream";
        }
        return label.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
