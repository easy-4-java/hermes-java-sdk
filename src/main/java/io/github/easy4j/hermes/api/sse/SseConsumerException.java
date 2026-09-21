package io.github.easy4j.hermes.api.sse;

/** Signals a business SSE consumer failure without misclassifying it as JSON decoding. */
public final class SseConsumerException extends RuntimeException {
    private final String eventId;
    private final String eventName;

    public SseConsumerException(SseFrame frame, Throwable cause) {
        super("Hermes SSE consumer failed for event "
                + (frame.getEvent() == null ? "<unnamed>" : frame.getEvent()), cause);
        this.eventId = frame.getId();
        this.eventName = frame.getEvent();
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
}
