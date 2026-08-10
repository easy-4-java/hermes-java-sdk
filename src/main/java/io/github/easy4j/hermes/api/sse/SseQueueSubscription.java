package io.github.easy4j.hermes.api.sse;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * <p>Hermes SSE 有界事件队列订阅。</p>
 *
 * <p>将事件队列与其订阅生命周期组合，调用方必须关闭句柄以释放底层连接。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class SseQueueSubscription implements AutoCloseable {

    /**
     * 保存 SSE 事件的有界阻塞队列。
     */
    private final BlockingQueue<SseEvent> queue;
    /**
     * 队列对应的 SSE 订阅句柄。
     */
    private final SseSubscription subscription;

    /**
     * <p>创建 SseQueueSubscription 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param queue 接收事件的有界队列
     * @param subscription 队列对应的订阅句柄
     * @since 1.0.0
     */
    public SseQueueSubscription(BlockingQueue<SseEvent> queue, SseSubscription subscription) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.subscription = Objects.requireNonNull(subscription, "subscription");
    }

    /**
     * <p>返回接收 SSE 事件的有界队列。</p>
     *
     * @return 接收 SSE 事件的有界队列
     * @since 1.0.0
     */
    public BlockingQueue<SseEvent> getQueue() {
        return queue;
    }

    /**
     * <p>返回队列对应的订阅句柄。</p>
     *
     * @return 可用于取消和关闭底层连接的订阅句柄
     * @since 1.0.0
     */
    public SseSubscription getSubscription() {
        return subscription;
    }

    /**
     * <p>关闭当前对象并释放其拥有的资源。</p>
     *
     * <p>关闭组合订阅句柄；队列对象仍由调用方持有。</p>
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        subscription.close();
    }
}
