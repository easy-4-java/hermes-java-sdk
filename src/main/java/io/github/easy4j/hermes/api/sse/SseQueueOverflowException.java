package io.github.easy4j.hermes.api.sse;

/**
 * Signals that a bounded SSE queue cannot accept another event without losing data.
 *
 * @since 1.0.0
 */
public final class SseQueueOverflowException extends RuntimeException {

    public SseQueueOverflowException(String message) {
        super(message);
    }
}
