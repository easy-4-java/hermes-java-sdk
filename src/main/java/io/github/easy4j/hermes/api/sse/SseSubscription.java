package io.github.easy4j.hermes.api.sse;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 可取消且幂等关闭的 Hermes SSE 订阅句柄。 */
public final class SseSubscription implements AutoCloseable {

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Runnable cancellation;

    public SseSubscription(Runnable cancellation) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
    }

    /** 取消订阅；仅首次调用执行底层取消逻辑。 */
    public boolean cancel() {
        if (!active.compareAndSet(true, false)) {
            return false;
        }
        cancellation.run();
        return true;
    }

    /** 返回订阅是否仍处于活动状态。 */
    public boolean isActive() {
        return active.get();
    }

    @Override
    public void close() {
        cancel();
    }
}
