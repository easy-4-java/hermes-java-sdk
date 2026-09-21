package io.github.easy4j.hermes.api.sse;

/**
 * Describes whether event continuity across the current observation has been proven.
 *
 * @since 1.0.0
 */
public enum SseContinuityStatus {
    /** The subscription is still on its original transport connection. */
    INITIAL,
    /** The subscription reattached without a verified replay cursor or equivalent proof. */
    UNVERIFIED,
    /** The server contract explicitly proved replay continuity. */
    VERIFIED
}
