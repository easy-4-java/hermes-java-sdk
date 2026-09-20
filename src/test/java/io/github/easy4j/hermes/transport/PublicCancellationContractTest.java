package io.github.easy4j.hermes.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.HermesOkHttpClientFactory;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.ChatResponse;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicCancellationContractTest {

    @Test
    void cancellingPublicAsyncResultCancelsUnderlyingOkHttpCall() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBodyDelay(5, TimeUnit.SECONDS)
                    .setBody("{\"id\":\"chat-id\",\"choices\":[]}"));

            HermesHttpClientConfig config = new HermesHttpClientConfig();
            config.markUnsafeBaseUrlOverriddenForTest(true);
            config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));

            OkHttpClient transport = new OkHttpClient();
            try (HermesHttpClient http =
                         new HermesHttpClient(config, new ObjectMapper(), transport)) {
                ChatRequest request = new ChatRequest();
                request.setMessages(Collections.singletonList(
                        new ChatRequest.Message("user", "hello")));

                CompletableFuture<ChatResponse> future = http.chatCompletionAsync(request);

                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                assertTrue(transport.dispatcher().runningCallsCount() > 0);
                assertTrue(future.cancel(true));
                assertTrue(future.isCancelled());

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (transport.dispatcher().runningCallsCount() != 0
                        && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                assertEquals(0, transport.dispatcher().runningCallsCount(),
                        "cancelling the public future must cancel its underlying OkHttp Call");
            } finally {
                HermesOkHttpClientFactory.shutdown(transport);
            }
        }
    }
}
