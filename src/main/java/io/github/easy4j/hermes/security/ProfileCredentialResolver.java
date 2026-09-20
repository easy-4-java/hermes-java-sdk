package io.github.easy4j.hermes.security;

/** Resolves the credential binding for a named Hermes profile. */
@FunctionalInterface
public interface ProfileCredentialResolver {
    ProfileBinding resolve(String profileId);
}
