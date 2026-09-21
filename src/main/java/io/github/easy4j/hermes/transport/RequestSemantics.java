package io.github.easy4j.hermes.transport;

/**
 * Classifies request behavior so transport recovery cannot accidentally replay side-effecting work.
 *
 * @since 1.0.0
 */
public enum RequestSemantics {
    /** A read may be retried only by HTTP retry policy; it is not an SSE observation reattach. */
    READ(true, false),
    /** Creating agent work must never be replayed merely because response observation failed. */
    AGENT_CREATE(false, false),
    /** Side-effecting control writes require an explicit verified idempotency contract before replay. */
    CONTROL_WRITE(false, false),
    /** Re-observing an already existing run may reconnect without creating new work. */
    OBSERVE(false, true);

    private final boolean transportReplaySafe;
    private final boolean observationReattachAllowed;

    RequestSemantics(boolean transportReplaySafe, boolean observationReattachAllowed) {
        this.transportReplaySafe = transportReplaySafe;
        this.observationReattachAllowed = observationReattachAllowed;
    }

    public boolean isTransportReplaySafe() {
        return transportReplaySafe;
    }

    public boolean isObservationReattachAllowed() {
        return observationReattachAllowed;
    }
}
