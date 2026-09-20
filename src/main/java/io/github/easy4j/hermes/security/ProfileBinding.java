package io.github.easy4j.hermes.security;

import java.util.Objects;

/** Immutable binding between a named profile and a non-root credential provider. */
public final class ProfileBinding {
    private final String profileId;
    private final String credentialIdentity;
    private final CredentialProvider credentialProvider;

    private ProfileBinding(String profileId, String credentialIdentity,
                           CredentialProvider credentialProvider) {
        this.profileId = requireText(profileId, "profileId");
        this.credentialIdentity = requireText(credentialIdentity, "credentialIdentity");
        this.credentialProvider = Objects.requireNonNull(credentialProvider, "credentialProvider");
    }

    public static ProfileBinding of(String profileId, String credentialIdentity,
                                    CredentialProvider credentialProvider) {
        return new ProfileBinding(profileId, credentialIdentity, credentialProvider);
    }

    public String getProfileId() { return profileId; }
    public String getCredentialIdentity() { return credentialIdentity; }
    public CredentialProvider getCredentialProvider() { return credentialProvider; }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    @Override
    public String toString() {
        return "ProfileBinding{profileId='" + profileId + "', credentialIdentity='"
                + credentialIdentity + "'}";
    }
}
