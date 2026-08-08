package io.github.easy4j.hermes;

import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.ChatStreamingResponse;
import io.github.easy4j.hermes.api.model.ResponseRequest;
import io.github.easy4j.hermes.api.model.SseEvent;
import io.github.easy4j.hermes.util.HermesJsonParser;
import io.github.easy4j.hermes.util.HermesObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
        ChatStreamingResponse stream = new ChatStreamingResponse().onDelta(deltas::append);
        stream.accept(chat);
        stream.accept(direct);
        stream.accept(event("{}"));
        assertEquals(3, stream.getEventQueue().size());
        assertEquals("hello world", stream.getAccumulatedContent());
        stream.finish();
        assertEquals("hello world", stream.get(1, TimeUnit.SECONDS));
        assertEquals("hello world", deltas.toString());

        ChatStreamingResponse failed = new ChatStreamingResponse();
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
            sse.stop();
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
            BlockingQueue<SseEvent> queue = sse.subscribeQueue("run-id");
            SseEvent runEvent = queue.poll(3, TimeUnit.SECONDS);
            assertNotNull(runEvent);
            assertEquals("hi", runEvent.deltaText());
            sse.stop();
        }

        AtomicReference<HermesSseClient> reference = new AtomicReference<>();
        CountDownLatch sessionLatch = new CountDownLatch(1);
        try (HermesSseClient sse = new HermesSseClient(config, null, client)) {
            reference.set(sse);
            sse.subscribeSessionStream("session-id", "hello", event -> {
                assertEquals("hi", event.deltaText());
                sessionLatch.countDown();
                reference.get().stop();
            });
            assertTrue(sessionLatch.await(3, TimeUnit.SECONDS));
        } finally {
            HermesOkHttpClientFactory.shutdown(client);
        }

        HermesSseClient owned = new HermesSseClient(config, null, null);
        owned.close();
    }

    private SseEvent event(String data) {
        SseEvent event = new SseEvent();
        event.setData(data);
        return event;
    }
}
