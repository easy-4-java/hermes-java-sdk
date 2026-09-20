package io.github.easy4j.hermes.security;

import java.util.Objects;

/** Immutable credential value resolved for a single request. */
public final class CredentialSnapshot {
    private final String token;
    private final String generation;

    private CredentialSnapshot(String token, String generation) {
        this.token = Objects.requireNonNull(token, "token");
        this.generation = generation;
    }

    public static CredentialSnapshot of(String token, String generation) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return new CredentialSnapshot(token, generation);
    }

    public String getToken() { return token; }
    public String getGeneration() { return generation; }

    @Override
    public String toString() {
        return "CredentialSnapshot{token=<redacted>, generation='" + generation + "'}";
    }
}
