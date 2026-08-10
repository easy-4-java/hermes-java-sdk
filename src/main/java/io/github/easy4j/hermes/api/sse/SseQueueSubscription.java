package io.github.easy4j.hermes.api.sse;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/** Hermes SSE 有界事件队列及其订阅生命周期的组合句柄。 */
public final class SseQueueSubscription implements AutoCloseable {

    private final BlockingQueue<SseEvent> queue;
    private final SseSubscription subscription;

    public SseQueueSubscription(BlockingQueue<SseEvent> queue, SseSubscription subscription) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.subscription = Objects.requireNonNull(subscription, "subscription");
    }

    /** 返回接收事件的有界队列。 */
    public BlockingQueue<SseEvent> getQueue() {
        return queue;
    }

    /** 返回队列对应的订阅句柄。 */
    public SseSubscription getSubscription() {
        return subscription;
    }

    @Override
    public void close() {
        subscription.close();
    }
}
