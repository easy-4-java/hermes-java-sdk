package io.github.easy4j.hermes;

import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hermes 基于 OkHttp enqueue/EventSource 的 500 并发回归测试。 */
class HermesNonBlockingConcurrencyTest {

    private static final int CONCURRENCY = 500;

    @Test
    void shouldCompleteFiveHundredAsyncRequestsWithBoundedOkHttpDispatcher() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            for (int index = 0; index < CONCURRENCY; index++) {
                server.enqueue(jsonResponse("{\"id\":\"chatcmpl-" + index
                        + "\",\"model\":\"hermes-agent\",\"choices\":[{\"index\":0,"
                        + "\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},"
                        + "\"finish_reason\":\"stop\"}]}"));
            }
            HermesHttpClientConfig config = config(server);
            long baselineThreads = countThreads("hermes-okhttp-dispatcher-");

            try (HermesClient client = new HermesClient(config)) {
                List<CompletableFuture<?>> futures = new ArrayList<>(CONCURRENCY);
                for (int index = 0; index < CONCURRENCY; index++) {
                    futures.add(client.chat().chatCompletionAsync(request()));
                }
                CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                        .get(30, TimeUnit.SECONDS);

                assertEquals(CONCURRENCY, server.getRequestCount());
                assertTrue(countThreads("hermes-okhttp-dispatcher-") - baselineThreads
                                <= config.getMaxRequests() * 2L,
                        "HTTP and SSE OkHttp dispatchers exceeded their configured bounds");
            }
        }
    }

    @Test
    void shouldCompleteFiveHundredSseResponsesWithoutConsumerThreadPerStream() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            for (int index = 0; index < CONCURRENCY; index++) {
                server.enqueue(new MockResponse().setResponseCode(200)
                        .setHeader("Content-Type", "text/event-stream")
                        .setBody("data: [DONE]\n\n"));
            }
            HermesHttpClientConfig config = config(server);

            try (HermesClient client = new HermesClient(config)) {
                List<CompletableFuture<?>> streams = new ArrayList<>(CONCURRENCY);
                for (int index = 0; index < CONCURRENCY; index++) {
                    StreamingChatResponse stream = client.chat().chatCompletionStream(request());
                    streams.add(stream);
                }
                CompletableFuture.allOf(streams.toArray(new CompletableFuture<?>[0]))
                        .get(30, TimeUnit.SECONDS);

                assertEquals(CONCURRENCY, server.getRequestCount());
                assertEquals(0L, countThreads("hermes-sse-consumer-"));
                assertTrue(countThreads("hermes-okhttp-dispatcher-") <= config.getMaxRequests() * 2L);
            }
        }
    }

    private HermesHttpClientConfig config(MockWebServer server) {
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
        config.setStartupCheckEnabled(false);
        config.setMaxRequests(64);
        config.setMaxRequestsPerHost(64);
        return config;
    }

    private ChatRequest request() {
        return new ChatRequest("hermes-agent",
                Collections.singletonList(new ChatRequest.Message("user", "ping")),
                false, null, null, null, null, null, null, null, null, null, null);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody(body);
    }

    private long countThreads(String prefix) {
        return Thread.getAllStackTraces().keySet().stream().map(Thread::getName)
                .filter(name -> name.startsWith(prefix)).count();
    }
}
