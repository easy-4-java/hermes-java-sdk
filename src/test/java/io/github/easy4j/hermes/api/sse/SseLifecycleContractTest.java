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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.extension.logging.HttpLogLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseLifecycleContractTest {

    @Test
    void standardChatChunkDoesNotRequireNestedDataField() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            String body = "data: {\"choices\":[{\"delta\":{\"content\":\"你好 \"}}]}\n\n"
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
            assertEquals("你好 ", received.get().deltaText());
            assertEquals("{\"choices\":[{\"delta\":{\"content\":\"你好 \"}}]}",
                    received.get().getData());
        }
    }

    @Test
    void malformedJsonAndConsumerFailureRemainIsolated() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            String body = "data: invalid\n\n"
                    + "data: {\"delta\":\"hello\"}\n\n"
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
        config.getDebug().setEnabled(true);
        config.getDebug().setLevel(HttpLogLevel.BODY);
        config.getDebug().setMaxContentLength(4);

        ChatRequest request = new ChatRequest();
        request.setMessages(Collections.singletonList(new ChatRequest.Message("user", "hello")));
        AtomicInteger consumerCalls = new AtomicInteger();

        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            CountDownLatch failed = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            SseSubscription subscription = sse.subscribeChat(request, ignored -> {
                consumerCalls.incrementAndGet();
                throw new IllegalStateException("consumer failure");
            }, () -> { }, error -> {
                failure.set(error);
                failed.countDown();
            });

            assertTrue(failed.await(3, TimeUnit.SECONDS));
            assertEquals(1, consumerCalls.get(),
                    "malformed JSON must not be delivered to the business consumer");
            assertTrue(failure.get() instanceof SseConsumerException,
                    "consumer callback failures must not be reported as JSON parsing failures");
            assertFalse(subscription.isActive());
            assertEquals(0, sse.activeSubscriptionCount());
        }
    }

    @Test
    void multiLineCrLfCommentAndFragmentedUtf8PreserveRawFrame() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            String body = ": keepalive\r\n"
                    + "id: evt-1\r\n"
                    + "event: assistant.delta\r\n"
                    + "data: {\"choices\":[{\"delta\":\r\n"
                    + "data: {\"content\":\"你 好 \"}}]}\r\n\r\n"
                    + "data: [DONE]\r\n\r\n";
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setChunkedBody(body, 1));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            ChatRequest request = new ChatRequest();
            request.setMessages(Collections.singletonList(
                    new ChatRequest.Message("user", "hello")));

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                CountDownLatch complete = new CountDownLatch(1);
                AtomicReference<SseEvent> received = new AtomicReference<>();
                sse.subscribeChat(request, received::set, complete::countDown, ignored -> { });

                assertTrue(complete.await(3, TimeUnit.SECONDS));
                SseEvent event = received.get();
                assertNotNull(event);
                assertEquals("evt-1", event.getId());
                assertEquals("assistant.delta", event.getEvent());
                assertEquals("你 好 ", event.deltaText());
                assertTrue(event.getData().contains("\n"),
                        "multiple data lines must remain visible in the raw SSE data");
            }
        }
    }

    @Test
    void unknownEventPreservesServerIdentityAndRawData() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            String body = "id: mystery-7\n"
                    + "event: mystery.data\n"
                    + "data: {\"foo\":\"bar\"}\n\n"
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
            assertEquals("mystery-7", received.get().getId());
            assertEquals("mystery.data", received.get().getEvent());
            assertEquals("{\"foo\":\"bar\"}", received.get().getData());
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

    @Test
    void runIdentifierIsEncodedAsOneSsePathSegment() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: [DONE]\\n\\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription subscription =
                        sse.subscribeRunEvents("run/alpha?x=1", ignored -> { });
                okhttp3.mockwebserver.RecordedRequest request =
                        server.takeRequest(3, TimeUnit.SECONDS);
                assertNotNull(request);
                assertEquals("/v1/runs/run%2Falpha%3Fx%3D1/events", request.getPath());
                subscription.cancel();
            }
        }
    }

}
