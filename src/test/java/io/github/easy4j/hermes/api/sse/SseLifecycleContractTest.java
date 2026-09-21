package io.github.easy4j.hermes.api.sse;

import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.api.HermesChatClient;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.extension.logging.HttpLogLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void oversizedFrameFailsBeforeBusinessDelivery() throws Exception {
        StringBuilder payload = new StringBuilder("{\"delta\":\"");
        for (int i = 0; i < 128; i++) {
            payload.append('x');
        }
        payload.append("\"}");

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            String body = "data: " + payload + "\n\n";
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
        config.setStreamMaxEventBytes(32);

        ChatRequest request = new ChatRequest();
        request.setMessages(Collections.singletonList(new ChatRequest.Message("user", "hello")));
        AtomicInteger consumerCalls = new AtomicInteger();

        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            CountDownLatch failed = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            SseSubscription subscription = sse.subscribeChat(request,
                    ignored -> consumerCalls.incrementAndGet(),
                    () -> { },
                    error -> {
                        failure.set(error);
                        failed.countDown();
                    });

            assertTrue(failed.await(3, TimeUnit.SECONDS));
            assertEquals(0, consumerCalls.get());
            assertTrue(failure.get() instanceof SseProtocolException);
            assertFalse(subscription.isActive());
            assertEquals(0, sse.activeSubscriptionCount());
        }
    }

    @Test
    void eofWithoutTerminalFailsStreamingChatButKeepsPartialContent() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());

            ChatRequest request = new ChatRequest();
            request.setMessages(Collections.singletonList(
                    new ChatRequest.Message("user", "hello")));

            try (HermesChatClient chat = new HermesChatClient(config)) {
                StreamingChatResponse stream = chat.chatCompletionStream(request);
                ExecutionException failure = assertThrows(ExecutionException.class,
                        () -> stream.get(3, TimeUnit.SECONDS));
                assertTrue(failure.getCause() instanceof SseStreamInterruptedException);
                assertEquals("partial", stream.getAccumulatedContent());
                assertEquals(1, server.getRequestCount());
            }
        }
    }

    @Test
    void doneThenEofCompletesExactlyOnceWithoutReplay() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: [DONE]\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            ChatRequest request = new ChatRequest();
            request.setMessages(Collections.singletonList(
                    new ChatRequest.Message("user", "hello")));

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                CountDownLatch completed = new CountDownLatch(1);
                AtomicInteger completionCalls = new AtomicInteger();
                AtomicReference<Throwable> failure = new AtomicReference<>();
                SseSubscription subscription = sse.subscribeChat(request,
                        ignored -> { },
                        () -> {
                            completionCalls.incrementAndGet();
                            completed.countDown();
                        },
                        failure::set);

                assertTrue(completed.await(3, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(250);
                while (subscription.isActive() && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                assertFalse(subscription.isActive());
                assertEquals(1, completionCalls.get());
                assertEquals(null, failure.get());
                assertEquals(1, server.getRequestCount());
            }
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


    @Test
    void runReattachWithoutCursorMarksContinuityUnverifiedAndDoesNotCreateRun() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: evt-a\ndata: {\"delta\":\"first\"}\n\n"));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: evt-b\ndata: {\"delta\":\"second\"}\n\ndata: [DONE]\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            config.setStreamReconnectMaxAttempts(1);
            config.setStreamReconnectInitialDelayMillis(1);
            config.setStreamReconnectMaxDelayMillis(1);

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription subscription = sse.subscribeRunEvents("run-existing", ignored -> { });

                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (subscription.isActive() && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                assertFalse(subscription.isActive());
                assertEquals(SseContinuityStatus.UNVERIFIED, subscription.getContinuityStatus());
                assertEquals(2, server.getRequestCount());
            }
        }
    }

    @Test
    void repeatedTextWithDifferentServerEventIdsIsPreservedAcrossReattach() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: evt-1\ndata: {\"delta\":\"same\"}\n\n"));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: evt-2\ndata: {\"delta\":\"same\"}\n\ndata: [DONE]\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            config.setStreamReconnectMaxAttempts(1);
            config.setStreamReconnectInitialDelayMillis(1);
            config.setStreamReconnectMaxDelayMillis(1);

            java.util.List<SseEvent> received =
                    java.util.Collections.synchronizedList(new java.util.ArrayList<SseEvent>());

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription subscription = sse.subscribeRunEvents("run-existing", received::add);

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (subscription.isActive() && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                assertFalse(subscription.isActive());
                assertEquals(2, received.size());
                assertEquals("same", received.get(0).deltaText());
                assertEquals("same", received.get(1).deltaText());
                assertEquals("evt-1", received.get(0).getId());
                assertEquals("evt-2", received.get(1).getId());
                assertEquals(SseContinuityStatus.UNVERIFIED, subscription.getContinuityStatus());
            }
        }
    }


    @Test
    void chatTransportFailureDoesNotReplayWithoutVerifiedIdempotency() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("failure"));
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: [DONE]\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            config.setStreamReconnectMaxAttempts(3);
            config.setStreamReconnectInitialDelayMillis(1);
            config.setStreamReconnectMaxDelayMillis(1);

            ChatRequest request = new ChatRequest();
            request.setMessages(Collections.singletonList(
                    new ChatRequest.Message("user", "hello")));

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                CountDownLatch failed = new CountDownLatch(1);
                AtomicReference<Throwable> failure = new AtomicReference<>();
                SseSubscription subscription = sse.subscribeChat(
                        request, ignored -> { }, () -> { }, error -> {
                            failure.set(error);
                            failed.countDown();
                        });

                assertTrue(failed.await(3, TimeUnit.SECONDS));
                assertNotNull(failure.get());

                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(250);
                while (server.getRequestCount() < 2 && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                assertFalse(subscription.isActive());
                assertEquals(1, server.getRequestCount(),
                        "agent-creation writes must not replay without verified idempotency");
            }
        }
    }


    @Test
    void saturatedQueueFailsExplicitlyWithoutDroppingOldestEvent() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: evt-1\ndata: {\"delta\":\"first\"}\n\n"
                            + "id: evt-2\ndata: {\"delta\":\"second\"}\n\n"
                            + "data: [DONE]\n\n"));
            server.start();

            HermesHttpClientConfig config = new HermesHttpClientConfig()
                    .setEndpointPolicy(io.github.easy4j.hermes.security.EndpointPolicy
                            .trustedLocal("127.0.0.1", server.getPort()))
                    .setBaseUrl("http://127.0.0.1:" + server.getPort());
            config.setStreamEventQueueCapacity(1);
            config.setStreamReconnectMaxAttempts(0);

            try (HermesSseClient sse = new HermesSseClient(config, null, null);
                 SseQueueSubscription queued = sse.subscribeRunEventsQueue("run-overflow")) {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (queued.getSubscription().isActive() && System.nanoTime() < deadline) {
                    Thread.yield();
                }

                assertFalse(queued.getSubscription().isActive());
                assertTrue(queued.getSubscription().getTerminalError()
                                instanceof SseQueueOverflowException,
                        "queue saturation must be reported explicitly");
                assertEquals(1, queued.getQueue().size());
                assertEquals("evt-1", queued.getQueue().peek().getId(),
                        "overflow must not silently discard the oldest undelivered event");
            }
        }
    }

}
