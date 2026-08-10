package io.github.easy4j.hermes.api.sse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>Hermes 流式聊天响应。</p>
 *
 * <p>逐段累积文本、向业务回调增量，并通过 CompletableFuture 表达完成、失败与取消。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class StreamingChatResponse extends CompletableFuture<String> {

    /**
     * 已接收文本增量的可变累积缓冲区。
     */
    private final StringBuilder content = new StringBuilder();
    /**
     * 每次收到文本增量时调用的业务消费者。
     */
    private Consumer<String> deltaConsumer;
    /**
     * 底层流取消动作的原子引用。
     */
    private final AtomicReference<Runnable> cancellation = new AtomicReference<>();

    /**
     * <p>创建 StreamingChatResponse 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @since 1.0.0
     */
    public StreamingChatResponse() {
    }

    /**
     * <p>注册文本增量消费回调。</p>
     *
     * @param consumer 事件或增量消费者，不得为 {@code null}
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse onDelta(Consumer<String> consumer) {
        this.deltaConsumer = consumer;
        return this;
    }

    /**
     * <p>消费一个 SSE 事件并累积文本增量。</p>
     *
     * @param event 待消费的 SSE 事件
     * @since 1.0.0
     */
    public void accept(SseEvent event) {
        String delta = event.deltaText();
        if (delta != null) {
            // 先写入完整响应缓冲区再通知消费者，保证回调观察到的累计内容包含当前分片。
            content.append(delta);
            if (deltaConsumer != null) {
                deltaConsumer.accept(delta);
            }
        }
    }

    /**
     * <p>以当前累积文本完成流式响应。</p>
     *
     * @since 1.0.0
     */
    public void finish() {
        complete(content.toString());
    }

    /**
     * <p>以异常结束流式响应。</p>
     *
     * @param error 导致流失败的异常
     * @since 1.0.0
     */
    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    /**
     * <p>返回当前已累积的文本。</p>
     *
     * @return 截至调用时已经接收并拼接的文本
     * @since 1.0.0
     */
    public String getAccumulatedContent() {
        return content.toString();
    }

    /**
     * <p>绑定底层 SSE 取消动作。</p>
     *
     * @param action 底层取消动作
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse onCancel(Runnable action) {
        cancellation.set(action);
        if (isCancelled() && Objects.nonNull(action)) {
            action.run();
        }
        return this;
    }

    /**
     * <p>取消流式聊天响应并关闭底层 SSE 订阅。</p>
     *
     * <p>取消具有幂等性，仅首次调用执行底层清理逻辑。</p>
     *
     * @param mayInterruptIfRunning 是否允许中断正在运行的任务
     * @return 首次成功取消返回 {@code true}，否则返回 {@code false}
     * @since 1.0.0
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // getAndSet 保证底层订阅只取消一次，随后再按 CompletableFuture 标准语义更新状态。
        Runnable action = cancellation.getAndSet(null);
        if (Objects.nonNull(action)) {
            action.run();
        }
        return super.cancel(mayInterruptIfRunning);
    }
}
