package io.github.easy4j.hermes.security;

import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.HermesCliConfig;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.model.ChatRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileAuthenticationContractTest {

    @Test
    void independentProfilesUseTheirOwnCredentials() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(okHealth());
            server.enqueue(okHealth());
            server.start();

            try (HermesClient root = new HermesClient(http(server), disabledCli())) {
                ProfileBinding teamA = ProfileBinding.of(
                        "team-a", "credential-a",
                        identity -> CredentialSnapshot.of("token-a", "generation-a"));
                ProfileBinding teamB = ProfileBinding.of(
                        "team-b", "credential-b",
                        identity -> CredentialSnapshot.of("token-b", "generation-b"));

                root.forProfile(teamA).health();
                root.forProfile(teamB).health();

                RecordedRequest first = server.takeRequest(3, TimeUnit.SECONDS);
                RecordedRequest second = server.takeRequest(3, TimeUnit.SECONDS);
                assertNotNull(first);
                assertNotNull(second);
                assertEquals("/p/team-a/health", first.getPath());
                assertEquals("Bearer token-a", first.getHeader("Authorization"));
                assertEquals("/p/team-b/health", second.getPath());
                assertEquals("Bearer token-b", second.getHeader("Authorization"));
            }
        }
    }

    @Test
    void configuredResolverProvidesNamedProfileCredentials() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(okHealth());
            server.start();

            HermesClientConfig config = new HermesClientConfig();
            config.getHttp()
                    .setEndpointPolicy(EndpointPolicy.trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            config.getCli().setEnabled(false);
            config.setProfileCredentialResolver(profileId -> ProfileBinding.of(
                    profileId, "resolver-" + profileId,
                    identity -> CredentialSnapshot.of("resolved-token", "1")));

            try (HermesClient root = new HermesClient(config)) {
                root.forProfile("team-a").health();
                RecordedRequest request = server.takeRequest(3, TimeUnit.SECONDS);
                assertNotNull(request);
                assertEquals("/p/team-a/health", request.getPath());
                assertEquals("Bearer resolved-token", request.getHeader("Authorization"));
            }
        }
    }

    @Test
    void missingNamedProfileCredentialDoesNotBorrowRootApiKey() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            HermesHttpClientConfig http = http(server);
            http.setApiKey("root-secret");

            try (HermesClient root = new HermesClient(http, disabledCli())) {
                assertThrows(IllegalStateException.class, () -> root.forProfile("team-a"));
                assertEquals(0, server.getRequestCount(),
                        "missing profile credentials must fail before any request is sent");
            }
        }
    }

    @Test
    void credentialProviderIsResolvedForEveryNewRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(okHealth());
            server.enqueue(okHealth());
            server.start();

            AtomicReference<CredentialSnapshot> credential =
                    new AtomicReference<>(CredentialSnapshot.of("token-a", "generation-a"));
            ProfileBinding binding = ProfileBinding.of(
                    "team-a", "credential-a", identity -> credential.get());

            try (HermesClient root = new HermesClient(http(server), disabledCli())) {
                HermesClient profile = root.forProfile(binding);
                profile.health();

                credential.set(CredentialSnapshot.of("token-b", "generation-b"));
                profile.health();

                RecordedRequest first = server.takeRequest(3, TimeUnit.SECONDS);
                RecordedRequest second = server.takeRequest(3, TimeUnit.SECONDS);
                assertNotNull(first);
                assertNotNull(second);
                assertEquals("Bearer token-a", first.getHeader("Authorization"));
                assertEquals("Bearer token-b", second.getHeader("Authorization"));
            }
        }
    }

    @Test
    void credentialSnapshotDoesNotExposeSecretInToString() {
        CredentialSnapshot snapshot = CredentialSnapshot.of("super-secret-token", "generation-a");
        assertFalse(snapshot.toString().contains("super-secret-token"));
    }

    @Test
    void businessHeadersCannotOverrideBoundAuthorization() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            ProfileBinding binding = ProfileBinding.of(
                    "team-a", "credential-a",
                    identity -> CredentialSnapshot.of("profile-token", "1"));

            try (HermesClient root = new HermesClient(http(server), disabledCli())) {
                HermesClient profile = root.forProfile(binding);
                ChatRequest request = new ChatRequest();
                request.setMessages(Collections.singletonList(
                        new ChatRequest.Message("user", "hello")));

                assertThrows(IllegalArgumentException.class,
                        () -> profile.chatCompletion(
                                request,
                                Collections.singletonMap("Authorization", "Bearer attacker-token")));
                assertEquals(0, server.getRequestCount(),
                        "identity override must be rejected before network I/O");
            }
        }
    }

    private static HermesHttpClientConfig http(MockWebServer server) {
        return new HermesHttpClientConfig()
                .setEndpointPolicy(EndpointPolicy.trustedLocal("127.0.0.1", server.getPort()))
                .setBaseUrl("http://127.0.0.1:" + server.getPort());
    }

    private static HermesCliConfig disabledCli() {
        HermesCliConfig cli = new HermesCliConfig();
        cli.setEnabled(false);
        return cli;
    }

    private static MockResponse okHealth() {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"ok\"}");
    }
}
