package io.github.easy4j.hermes.api.sse;

/** Immutable raw SSE frame captured before endpoint-specific decoding. */
public final class SseFrame {
    private final String id;
    private final String event;
    private final String data;
    private final long receivedAtEpochMillis;

    public SseFrame(String id, String event, String data, long receivedAtEpochMillis) {
        this.id = id;
        this.event = event;
        this.data = data;
        this.receivedAtEpochMillis = receivedAtEpochMillis;
    }

    public String getId() { return id; }
    public String getEvent() { return event; }
    public String getData() { return data; }
    public long getReceivedAtEpochMillis() { return receivedAtEpochMillis; }
}
