package io.github.easy4j.hermes.api.sse;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import lombok.Getter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Hermes 流式聊天响应，逐段回调并在结束时返回累积全文。
 */
public class StreamingChatResponse extends CompletableFuture<String> {

    private final StringBuilder content = new StringBuilder();
    private Consumer<String> deltaConsumer;
    private final AtomicReference<Runnable> cancellation = new AtomicReference<>();
    @Getter
    private final BlockingQueue<SseEvent> eventQueue;

    public StreamingChatResponse() {
        this(1_024);
    }

    public StreamingChatResponse(int eventQueueCapacity) {
        this.eventQueue = new ArrayBlockingQueue<>(Math.max(1, eventQueueCapacity));
    }

    public StreamingChatResponse onDelta(Consumer<String> consumer) {
        this.deltaConsumer = consumer;
        return this;
    }

    public void accept(SseEvent event) {
        if (!eventQueue.offer(event)) {
            eventQueue.poll();
            eventQueue.offer(event);
        }
        String delta = event.deltaText();
        if (delta != null) {
            content.append(delta);
            if (deltaConsumer != null) {
                deltaConsumer.accept(delta);
            }
        }
    }

    public void finish() {
        complete(content.toString());
    }

    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    public String getAccumulatedContent() {
        return content.toString();
    }

    /** 绑定底层 SSE 取消动作。 */
    public StreamingChatResponse onCancel(Runnable action) {
        cancellation.set(action);
        if (isCancelled() && Objects.nonNull(action)) {
            action.run();
        }
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        Runnable action = cancellation.getAndSet(null);
        if (Objects.nonNull(action)) {
            action.run();
        }
        return super.cancel(mayInterruptIfRunning);
    }
}
