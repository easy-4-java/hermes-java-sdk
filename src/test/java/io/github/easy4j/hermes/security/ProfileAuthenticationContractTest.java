package io.github.easy4j.hermes.security;

import io.github.easy4j.hermes.HermesCliConfig;
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileAuthenticationContractTest {

    @Test
    void missingNamedProfileCredentialDoesNotBorrowRootApiKey() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            HermesHttpClientConfig http = new HermesHttpClientConfig()
                    .setEndpointPolicy(EndpointPolicy.trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            http.setApiKey("root-secret");

            HermesCliConfig cli = new HermesCliConfig();
            cli.setEnabled(false);

            try (HermesClient root = new HermesClient(http, cli)) {
                assertThrows(IllegalStateException.class, () -> root.forProfile("team-a"));
                assertEquals(0, server.getRequestCount(),
                        "missing profile credentials must fail before any request is sent");
            }
        }
    }
}
