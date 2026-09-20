package io.github.easy4j.hermes.security;

import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.HermesHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
