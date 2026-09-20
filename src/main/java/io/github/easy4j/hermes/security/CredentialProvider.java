package io.github.easy4j.hermes.security;

/** Resolves the current credential for a named profile on every new request. */
@FunctionalInterface
public interface CredentialProvider {
    CredentialSnapshot resolve(ProfileIdentity identity);
}
