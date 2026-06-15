package io.github.hiwepy.hermes.api.model;

import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

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
public class ChatStreamingResponse extends CompletableFuture<String> {

    private final StringBuilder content = new StringBuilder();
    private Consumer<String> deltaConsumer;
    @Getter
    private final BlockingQueue<SseEvent> eventQueue = new LinkedBlockingQueue<>();

    public ChatStreamingResponse onDelta(Consumer<String> deltaConsumer) {
        this.deltaConsumer = deltaConsumer;
        return this;
    }

    /** Feed an SSE event into this stream. */
    public void accept(SseEvent event) {
        eventQueue.add(event);
        String delta = event.deltaText();
        if (delta != null) {
            content.append(delta);
            if (deltaConsumer != null) {
                deltaConsumer.accept(delta);
            }
        }
    }

    /** Signal stream completion. */
    public void finish() {
        complete(content.toString());
    }

    /** Signal stream error. */
    public void fail(Throwable error) {
        completeExceptionally(error);
    }

    public String getAccumulatedContent() { return content.toString(); }

}
