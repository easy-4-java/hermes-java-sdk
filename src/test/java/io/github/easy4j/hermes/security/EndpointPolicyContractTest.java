package io.github.easy4j.hermes.security;

import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.exception.HermesHttpException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndpointPolicyContractTest {

    @Test
    void testEp001S1() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{}"));
            server.start();

            int allowedPort = server.getPort();
            EndpointPolicy policy = EndpointPolicy.trustedLocal("127.0.0.1", allowedPort);

            HermesHttpClientConfig allowed = new HermesHttpClientConfig()
                    .setEndpointPolicy(policy)
                    .setBaseUrl("http://127.0.0.1:" + allowedPort);

            try (HermesHttpClient client = new HermesHttpClient(allowed)) {
                assertNotNull(client.health());
                assertNotNull(server.takeRequest());
            }

            HermesHttpClientConfig differentPort = new HermesHttpClientConfig()
                    .setEndpointPolicy(policy);
            assertThrows(IllegalArgumentException.class, () -> differentPort.setBaseUrl(
                    "http://127.0.0.1:" + (allowedPort + 1)));
        }
    }
    @Test
    void testEp002S2() {
        EndpointPolicy policy = EndpointPolicy.strictPublic();
        assertThrows(IllegalArgumentException.class, () ->
                policy.require("https://hermes-endpoint-does-not-exist.invalid"));
    }

    @Test
    void testEp003S1() throws Exception {
        try (MockWebServer origin = new MockWebServer();
             MockWebServer redirected = new MockWebServer()) {
            origin.start();
            redirected.start();

            origin.enqueue(new MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", "http://127.0.0.1:" + redirected.getPort() + "/redirected-health"));
            redirected.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{}"));

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(EndpointPolicy.trustedLocal("127.0.0.1", origin.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + origin.getPort());
            config.setApiKey("profile-secret");

            try (HermesHttpClient client = new HermesHttpClient(config)) {
                assertThrows(HermesHttpException.class, client::health);
            }

            assertNotNull(origin.takeRequest(1, TimeUnit.SECONDS));
            assertNull(redirected.takeRequest(250, TimeUnit.MILLISECONDS),
                    "cross-origin redirect must not be followed");
        }
    }


    @Test
    void testEp001S2TrustedPrivateRequiresExplicitPolicy() {
        assertThrows(IllegalArgumentException.class, () ->
                EndpointPolicy.strictPublic().require("http://10.20.30.40:8642"));

        EndpointPolicy policy = EndpointPolicy.trustedPrivate("10.20.30.40", 8642);
        assertDoesNotThrow(() -> policy.require("http://10.20.30.40:8642"));
        assertThrows(IllegalArgumentException.class, () ->
                policy.require("http://10.20.30.41:8642"));
        assertThrows(IllegalArgumentException.class, () ->
                policy.require("http://10.20.30.40:8643"));
    }

    @Test
    void testPublicPolicyRequiresHttps() {
        EndpointPolicy policy = EndpointPolicy.strictPublic();
        assertThrows(IllegalArgumentException.class, () ->
                policy.require("http://1.1.1.1/v1"));
        assertDoesNotThrow(() -> policy.require("https://1.1.1.1/v1"));
    }

    @Test
    void testEp002S1RejectsMixedResolutionCandidates() throws Exception {
        EndpointPolicy policy = EndpointPolicy.strictPublic(host -> new java.net.InetAddress[] {
                java.net.InetAddress.getByAddress(new byte[] {1, 1, 1, 1}),
                java.net.InetAddress.getByAddress(new byte[] {127, 0, 0, 1})
        });

        assertThrows(IllegalArgumentException.class, () ->
                policy.require("https://mixed.example/v1"));
    }

    @Test
    void testEp003RejectsUserInfo() {
        assertThrows(IllegalArgumentException.class, () ->
                EndpointPolicy.strictPublic().require("https://user:secret@1.1.1.1/v1"));
    }

    @Test
    void testEp003S2EncodesReservedRunIdentifierAsOnePathSegment() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{}"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(EndpointPolicy.trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());

            try (HermesHttpClient client = new HermesHttpClient(config)) {
                assertNotNull(client.getRun("run/alpha?x=1"));
            }

            okhttp3.mockwebserver.RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertNotNull(request);
            org.junit.jupiter.api.Assertions.assertEquals(
                    "/v1/runs/run%2Falpha%3Fx%3D1", request.getPath());
        }
    }

}
