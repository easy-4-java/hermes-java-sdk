package io.github.easy4j.hermes.api.model;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

/**
 * Convenience wrapper for SSE streaming — accumulates events and completes a
 * {@link CompletableFuture} with the full delta text.
 *
 * <pre>{@code
 * StreamingResponse stream = client.chatCompletionStream(req);
 * stream.onDelta(text -> System.out.print(text));
 * String full = stream.get();  // blocks
 * }</pre>
 */
@Deprecated
public class ChatStreamingResponse extends io.github.easy4j.hermes.api.sse.StreamingChatResponse {

    public ChatStreamingResponse() {
        this(1_024);
    }

    public ChatStreamingResponse(int eventQueueCapacity) {
        super(eventQueueCapacity);
    }

    public ChatStreamingResponse onDelta(Consumer<String> deltaConsumer) {
        super.onDelta(deltaConsumer);
        return this;
    }

    /** 二进制兼容旧事件类型。 */
    public void accept(SseEvent event) {
        super.accept(event);
    }

}
