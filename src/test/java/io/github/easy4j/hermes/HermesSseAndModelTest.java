package io.github.easy4j.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import io.github.easy4j.hermes.api.model.ResponseRequest;
import io.github.easy4j.hermes.api.sse.SseEvent;
import io.github.easy4j.hermes.api.sse.SseQueueSubscription;
import io.github.easy4j.hermes.api.sse.SseSubscription;
import io.github.easy4j.hermes.util.HermesJsonParser;
import io.github.easy4j.hermes.util.HermesObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HermesSseAndModelTest {

    @Test
    void shouldParseSseEventsAndAccumulateStreamingContent() throws Exception {
        SseEvent chat = event("{\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");
        SseEvent direct = event("{\"delta\":\" world\"}");
        SseEvent object = event("{\"delta\":{\"text\":\"!\"}}");
        assertEquals("hello", chat.deltaText());
        assertEquals(" world", direct.deltaText());
        assertEquals("!", object.deltaText());
        assertNotNull(chat.getDataAsMap());
        assertNotNull(chat.getDataAsNode());

        SseEvent invalid = event("invalid");
        assertNull(invalid.deltaText());
        assertNull(invalid.getDataAsMap());
        assertNull(invalid.getDataAsNode());
        invalid.setData(null);
        assertNull(invalid.deltaText());
        assertNull(invalid.getDataAsMap());
        assertNull(invalid.getDataAsNode());

        StringBuilder deltas = new StringBuilder();
        StreamingChatResponse stream = new StreamingChatResponse().onDelta(deltas::append);
        stream.accept(chat);
        stream.accept(direct);
        stream.accept(event("{}"));
        assertEquals("hello world", stream.getAccumulatedContent());
        stream.finish();
        assertEquals("hello world", stream.get(1, TimeUnit.SECONDS));
        assertEquals("hello world", deltas.toString());

        StreamingChatResponse failed = new StreamingChatResponse();
        failed.fail(new IOException("stream failed"));
        assertThrows(Exception.class, failed::get);
    }

    @Test
    void shouldParseJsonUsingAllFallbackStrategies() {
        HermesJsonParser parser = new HermesJsonParser();
        assertNull(parser.parseFromText(null));
        assertNull(parser.parseFromText(""));
        assertEquals(1, parser.parseFromText("```json\n{\"a\":1}\n```").get("a"));
        assertEquals(2, parser.parseFromText(" {\"b\":2} ").get("b"));
        assertEquals(3, parser.parseFromText("prefix {\"c\":3} suffix").get("c"));
        assertNull(parser.parseFromText("```json\ninvalid\n``` no json"));
        assertNotNull(HermesObjectMapper.INSTANCE);

        ResponseRequest request = new ResponseRequest();
        request.setModel("m");
        request.setInput("input");
        request.setInstructions("instructions");
        request.setStore(true);
        request.setPreviousResponseId("previous");
        request.setConversation("conversation");
        request.setMaxOutputTokens(10);
        request.setTemperature(0.2);
        request.setTopP(0.9);
        request.setUser("user");
        ResponseRequest streamed = request.withStream();
        assertTrue(streamed.getStream());
        assertEquals(request.getModel(), streamed.getModel());
    }

    @Test
    void shouldHandleChatSseSuccessHeadersAndErrors() throws Exception {
        AtomicBoolean errorMode = new AtomicBoolean();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> customHeader = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            auth.set(chain.request().header("Authorization"));
            customHeader.set(chain.request().header("X-Custom"));
            int code = errorMode.get() ? 500 : 200;
            String body = errorMode.get() ? "failure" :
                    "event: assistant.delta\n" +
                    "data: {\"data\":\"{\\\"delta\\\":\\\"hi\\\"}\"}\n\n" +
                    "data: invalid\n\n" +
                    "data: [DONE]\n\n";
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(code).message(code == 200 ? "OK" : "Failure")
                    .body(ResponseBody.create(body, MediaType.get("text/event-stream"))).build();
        }).build();
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        config.setApiKey("key");
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatRequest.Message("user", "hello")));

        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            CountDownLatch complete = new CountDownLatch(1);
            AtomicReference<SseEvent> event = new AtomicReference<>();
            sse.subscribeChat(request, Map.of("X-Custom", "value", "Ignored", "ok"),
                    event::set, complete::countDown, ignored -> { });
            assertTrue(complete.await(3, TimeUnit.SECONDS));
            assertEquals("assistant.delta", event.get().getEvent());
            assertEquals("hi", event.get().deltaText());
            assertEquals("Bearer key", auth.get());
            assertEquals("value", customHeader.get());

            errorMode.set(true);
            CountDownLatch errorLatch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            sse.subscribeChat(request, ignored -> { }, () -> { }, value -> {
                error.set(value);
                errorLatch.countDown();
            });
            assertTrue(errorLatch.await(3, TimeUnit.SECONDS));
            assertNotNull(error.get());
        } finally {
            HermesOkHttpClientFactory.shutdown(client);
        }
    }

    @Test
    void shouldSubscribeToRunQueueAndSessionStreamIndependently() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(200).message("OK")
                        .body(ResponseBody.create(
                                "event: assistant.delta\ndata: {\"data\":\"{\\\"delta\\\":\\\"hi\\\"}\"}\n\n",
                                MediaType.get("text/event-stream")))
                        .build()).build();
        HermesHttpClientConfig config = new HermesHttpClientConfig();

        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            try (SseQueueSubscription subscription = sse.subscribeRunEventsQueue("run-id")) {
                SseEvent runEvent = subscription.getQueue().poll(3, TimeUnit.SECONDS);
                assertNotNull(runEvent);
                assertEquals("hi", runEvent.deltaText());
            }
        }

        AtomicReference<SseSubscription> reference = new AtomicReference<>();
        CountDownLatch sessionLatch = new CountDownLatch(1);
        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            SseSubscription subscription = sse.subscribeSessionEvents("session-id", "hello", event -> {
                assertEquals("hi", event.deltaText());
                sessionLatch.countDown();
                SseSubscription active = reference.get();
                if (active != null) {
                    active.cancel();
                }
            });
            reference.set(subscription);
            assertTrue(sessionLatch.await(3, TimeUnit.SECONDS));
            subscription.cancel();
        } finally {
            HermesOkHttpClientFactory.shutdown(client);
        }

        HermesSseClient owned = new HermesSseClient(config, null, null);
        owned.close();
    }

    @Test
    void shouldReconnectRunEventsAndCloseAllActiveSubscriptions() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(500).setBody("first failure"));
            server.enqueue(new MockResponse().setResponseCode(500).setBody("second failure"));
            HermesHttpClientConfig config = new HermesHttpClientConfig();
            config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
            config.setStreamReconnectMaxAttempts(1);
            config.setStreamReconnectInitialDelayMillis(1);
            config.setStreamReconnectMaxDelayMillis(1);

            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription subscription = sse.subscribeRunEvents("run-retry", event -> { });
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (sse.activeSubscriptionCount() != 0 && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                assertEquals(0, sse.activeSubscriptionCount());
                assertFalse(subscription.isActive());
            }
        }

        HermesHttpClientConfig invalidConfig = new HermesHttpClientConfig();
        invalidConfig.setBaseUrl("not-a-url");
        AtomicReference<Throwable> error = new AtomicReference<>();
        try (HermesSseClient sse = new HermesSseClient(invalidConfig, null, null)) {
            SseSubscription subscription = sse.subscribeChat(new ChatRequest(), event -> { },
                    () -> { }, error::set);
            assertNotNull(error.get());
            assertFalse(subscription.isActive());
        }
    }

    @Test
    void shouldKeepLatestQueueEventAndExposeSubscriptionLifecycle() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: {\"data\":\"{\\\"delta\\\":\\\"first\\\"}\"}\n\n"
                            + "data: {\"data\":\"{\\\"delta\\\":\\\"latest\\\"}\"}\n\n"));
            HermesHttpClientConfig config = new HermesHttpClientConfig();
            config.setBaseUrl(server.url("").toString().replaceAll("/+$", ""));
            config.setStreamEventQueueCapacity(1);
            config.setStreamReconnectMaxAttempts(0);
            try (HermesSseClient sse = new HermesSseClient(config, null, null);
                 SseQueueSubscription queueSubscription =
                         sse.subscribeRunEventsQueue("run-queue")) {
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (sse.activeSubscriptionCount() != 0 && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                SseEvent event = queueSubscription.getQueue().poll(3, TimeUnit.SECONDS);
                assertNotNull(event);
                assertEquals("latest", event.deltaText());
                assertNotNull(queueSubscription.getSubscription());
            }

            server.enqueue(new MockResponse().setResponseCode(500).setBody("session failure"));
            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                sse.subscribeSessionEvents("session-failed", "hello", event -> { });
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (sse.activeSubscriptionCount() != 0 && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                assertEquals(0, sse.activeSubscriptionCount());
            }

            server.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: [DONE]\n\n"));
            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                SseSubscription completed = sse.subscribeSessionEvents(
                        "session-complete", "hello", event -> { });
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (completed.isActive() && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                assertFalse(completed.isActive());
            }

            server.enqueue(new MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBodyDelay(3, TimeUnit.SECONDS).setBody("data: [DONE]\n\n"));
            try (HermesSseClient sse = new HermesSseClient(config, null, null)) {
                sse.subscribeRunEvents("run-active", event -> { });
                assertEquals(1, sse.activeSubscriptionCount());
            }
        }
    }

    @Test
    void shouldCoverChatStreamingOverloadsAndCancellationPaths() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(200).message("OK")
                        .body(ResponseBody.create("data: [DONE]\n\n",
                                MediaType.get("text/event-stream"))).build()).build();
        HermesHttpClientConfig config = new HermesHttpClientConfig();
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatRequest.Message("user", "hello")));
        try (HermesChatClient chat = new HermesChatClient(config, new ObjectMapper(), client)) {
            assertEquals("", chat.chatCompletionStream(request, Map.of("X-Test", "true"))
                    .get(3, TimeUnit.SECONDS));
            assertEquals("", chat.chatCompletionStreamWithSession(request, "key")
                    .get(3, TimeUnit.SECONDS));
            assertEquals("", chat.chatCompletionStreamWithSession(request, "key", "session")
                    .get(3, TimeUnit.SECONDS));
            assertEquals("", chat.chatCompletionStreamWithSession(
                    request, "key", "session", ignored -> { }).get(3, TimeUnit.SECONDS));
        } finally {
            HermesOkHttpClientFactory.shutdown(client);
        }

        AtomicInteger cancellations = new AtomicInteger();
        StreamingChatResponse cancelBeforeBind = new StreamingChatResponse();
        cancelBeforeBind.cancel(false);
        cancelBeforeBind.onCancel(cancellations::incrementAndGet);
        StreamingChatResponse bindBeforeCancel = new StreamingChatResponse()
                .onCancel(cancellations::incrementAndGet);
        bindBeforeCancel.cancel(false);
        assertEquals(2, cancellations.get());
    }

    private SseEvent event(String data) {
        SseEvent event = new SseEvent();
        event.setData(data);
        return event;
    }
}
