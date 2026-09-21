package io.github.easy4j.hermes.api.sse;

/**
 * Signals that an SSE stream ended before the protocol produced a verified terminal marker.
 *
 * <p>The caller may still inspect any partial content accumulated before the interruption,
 * but the SDK must not report that partial content as a confirmed successful completion.</p>
 *
 * @since 1.0.0
 */
public final class SseStreamInterruptedException extends RuntimeException {

    public SseStreamInterruptedException(String message) {
        super(message);
    }

    public SseStreamInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
