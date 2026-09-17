package io.github.easy4j.hermes.api.sse;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>可取消的 Hermes SSE 订阅句柄。</p>
 *
 * <p>使用原子状态保证取消与关闭幂等，并在首次取消时执行底层清理动作。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class SseSubscription implements AutoCloseable {

    /**
     * 订阅当前是否活动的原子状态。
     */
    private final AtomicBoolean active = new AtomicBoolean(true);
    /**
     * 首次取消订阅时执行的底层清理动作。
     */
    private final Runnable cancellation;

    /**
     * <p>创建 SseSubscription 实例。</p>
     *
     * <p>业务取消信号会传播到底层 OkHttp Call，避免已无消费者的请求继续占用连接。</p>
     *
     * @param cancellation 底层取消动作，不得为 {@code null}
     * @since 1.0.0
     */
    public SseSubscription(Runnable cancellation) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
    }

    /**
     * <p>取消 SSE 订阅。</p>
     *
     * <p>取消具有幂等性，仅首次调用执行底层清理逻辑。</p>
     *
     * @return 首次成功取消返回 {@code true}，否则返回 {@code false}
     * @since 1.0.0
     */
    public boolean cancel() {
        if (!active.compareAndSet(true, false)) {
            return false;
        }
        cancellation.run();
        return true;
    }

    /**
     * <p>判断订阅是否仍处于活动状态。</p>
     *
     * @return 订阅处于活动状态时返回 {@code true}
     * @since 1.0.0
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * <p>关闭当前对象并释放其拥有的资源。</p>
     *
     * <p>等价于幂等取消订阅。</p>
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        cancel();
    }
}
