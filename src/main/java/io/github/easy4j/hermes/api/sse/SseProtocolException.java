package io.github.easy4j.hermes.api.sse;

/** Signals an SSE protocol or frame-bound violation separately from transport and consumer failures. */
public final class SseProtocolException extends RuntimeException {
    private final String eventId;
    private final String eventName;

    public SseProtocolException(SseFrame frame, String message) {
        super(message);
        this.eventId = frame == null ? null : frame.getId();
        this.eventName = frame == null ? null : frame.getEvent();
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
}
