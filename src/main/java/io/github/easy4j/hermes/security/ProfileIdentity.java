package io.github.easy4j.hermes.security;

import java.util.Objects;

/** Non-secret identity tuple used to scope a named Hermes profile credential. */
public final class ProfileIdentity {
    private final String endpointIdentity;
    private final String profileId;
    private final String credentialIdentity;

    public ProfileIdentity(String endpointIdentity, String profileId, String credentialIdentity) {
        this.endpointIdentity = requireText(endpointIdentity, "endpointIdentity");
        this.profileId = requireText(profileId, "profileId");
        this.credentialIdentity = requireText(credentialIdentity, "credentialIdentity");
    }

    public String getEndpointIdentity() { return endpointIdentity; }
    public String getProfileId() { return profileId; }
    public String getCredentialIdentity() { return credentialIdentity; }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    @Override
    public String toString() {
        return "ProfileIdentity{endpointIdentity='" + endpointIdentity + "', profileId='"
                + profileId + "', credentialIdentity='" + credentialIdentity + "'}";
    }
}
