package io.github.easy4j.hermes.api.sse;

import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.model.ChatRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseLifecycleContractTest {

    @Test
    void standardChatChunkDoesNotRequireNestedDataField() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            String body = "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}\n\n"
                    + "data: [DONE]\n\n";
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(body, MediaType.get("text/event-stream")))
                    .build();
        }).build();

        HermesHttpClientConfig config = new HermesHttpClientConfig();
        config.markUnsafeBaseUrlOverriddenForTest(true);
        ChatRequest request = new ChatRequest();
        request.setMessages(Collections.singletonList(new ChatRequest.Message("user", "hello")));

        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            CountDownLatch complete = new CountDownLatch(1);
            AtomicReference<SseEvent> received = new AtomicReference<>();

            sse.subscribeChat(request, received::set, complete::countDown, ignored -> { });

            assertTrue(complete.await(3, TimeUnit.SECONDS));
            assertNotNull(received.get());
            assertEquals("hello", received.get().deltaText());
        }
    }

    @Test
    void sessionDisconnectDoesNotReplayOriginalPost() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n"));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: [DONE]\n\n"));

            HermesHttpClientConfig config = new HermesHttpClientConfig();
            config.markUnsafeBaseUrlOverriddenForTest(true);
            config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
            config.setStreamReconnectMaxAttempts(1);
            config.setStreamReconnectInitialDelayMillis(1);
            config.setStreamReconnectMaxDelayMillis(1);

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription subscription =
                        sse.subscribeSessionEvents("session-id", "hello", ignored -> { });

                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));

                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
                while (server.getRequestCount() < 2 && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                subscription.cancel();
                assertEquals(1, server.getRequestCount(),
                        "disconnecting a session stream must not resubmit the original POST");
            }
        }
    }
}
