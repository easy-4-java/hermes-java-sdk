package io.github.easy4j.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hermes OkHttpClient 复用、配置与生命周期测试。
 */
class HermesOkHttpClientTest {

    @Test
    void shouldRouteManagedProfilesThroughOneSharedTransport() {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        OkHttpClient external = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    requestedPath.set(chain.request().url().encodedPath());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(
                                    "{\"id\":\"response-1\",\"choices\":[]}",
                                    MediaType.get("application/json")))
                            .build();
                })
                .build();
        HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
        httpConfig.setBaseUrl("http://127.0.0.1:8642/");
        HermesCliConfig cliConfig = new HermesCliConfig();
        cliConfig.setEnabled(false);

        try (HermesClient client = new HermesClient(httpConfig, cliConfig, new ObjectMapper(), external)) {
            HermesClient sales = client.forProfile("sales");
            assertSame(sales, client.forProfile("sales"));
            assertSame(external, sales.getOkHttpClient());
            assertFalse(sales.isCliEnabled());
            sales.chatCompletion(new ChatRequest());
            assertEquals("/p/sales/v1/chat/completions", requestedPath.get());
            assertThrows(IllegalArgumentException.class, () -> client.forProfile("../sales"));
            assertThrows(IllegalStateException.class, () -> sales.forProfile("care"));

            sales.close();
            assertSame(sales, client.forProfile("sales"));
        } finally {
            HermesOkHttpClientFactory.shutdown(external);
        }
    }

    @Test
    void shouldUseAndPreserveExternallyManagedOkHttpClient() {
        Dispatcher dispatcher = new Dispatcher();
        ConnectionPool connectionPool = new ConnectionPool(48, 10, TimeUnit.MINUTES);
        OkHttpClient external = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .build();
        HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
        httpConfig.setStartupCheckEnabled(false);
        HermesCliConfig cliConfig = new HermesCliConfig();
        cliConfig.setEnabled(false);

        HermesClient client = new HermesClient(httpConfig, cliConfig, external);
        assertSame(external, client.getOkHttpClient());
        client.close();

        assertFalse(dispatcher.executorService().isShutdown());
        HermesOkHttpClientFactory.shutdown(external);
    }

    @Test
    void shouldBuildConfiguredHighConcurrencyClientAndCloseOwnedResources() {
        HermesClientConfig config = new HermesClientConfig();
        config.getHttp().setStartupCheckEnabled(false);
        config.getHttp().setConnectTimeoutMillis(1_500);
        config.getHttp().setReadTimeoutMillis(90_000);
        config.getHttp().setWriteTimeoutMillis(8_000);
        config.getHttp().setCallTimeoutMillis(100_000);
        config.getHttp().setMaxIdleConnections(40);
        config.getHttp().setKeepAliveDurationMillis(420_000L);
        config.getHttp().setMaxRequests(160);
        config.getHttp().setMaxRequestsPerHost(80);
        config.getCli().setEnabled(false);

        OkHttpClient owned;
        try (HermesClient client = new HermesClient(config)) {
            owned = client.getOkHttpClient();
            assertEquals(1_500, owned.connectTimeoutMillis());
            assertEquals(90_000, owned.readTimeoutMillis());
            assertEquals(8_000, owned.writeTimeoutMillis());
            assertEquals(100_000, owned.callTimeoutMillis());
            assertEquals(160, owned.dispatcher().getMaxRequests());
            assertEquals(80, owned.dispatcher().getMaxRequestsPerHost());
        }
        assertTrue(owned.dispatcher().executorService().isShutdown());
    }

    @Test
    void shouldStreamConcurrentlyWithoutBlockingCallerOrMutatingRequest() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch releaseResponses = new CountDownLatch(1);
        AtomicInteger activeCalls = new AtomicInteger();
        AtomicInteger maxActiveCalls = new AtomicInteger();
        AtomicReference<String> requestJson = new AtomicReference<>();
        OkHttpClient external = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Buffer buffer = new Buffer();
                    chain.request().body().writeTo(buffer);
                    requestJson.set(buffer.readUtf8());
                    int active = activeCalls.incrementAndGet();
                    maxActiveCalls.accumulateAndGet(active, Math::max);
                    bothStarted.countDown();
                    try {
                        if (!releaseResponses.await(2, TimeUnit.SECONDS)) {
                            throw new IOException("test response release timed out");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("interrupted", e);
                    } finally {
                        activeCalls.decrementAndGet();
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "text/event-stream")
                            .body(ResponseBody.create("data: [DONE]\n\n", MediaType.get("text/event-stream")))
                            .build();
                })
                .build();
        HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
        httpConfig.setStartupCheckEnabled(false);
        HermesCliConfig cliConfig = new HermesCliConfig();
        cliConfig.setEnabled(false);
        ChatRequest request = new ChatRequest();
        request.setModel("hermes-agent");
        request.setMessages(List.of(new ChatRequest.Message("user", "ping")));

        try (HermesClient client = new HermesClient(httpConfig, cliConfig, new ObjectMapper(), external)) {
            long startedAt = System.nanoTime();
            StreamingChatResponse first = client.chatCompletionStream(request);
            StreamingChatResponse second = client.chatCompletionStream(request);
            long returnMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertTrue(returnMillis < 200L, "streaming calls blocked for " + returnMillis + "ms");
            assertTrue(bothStarted.await(1, TimeUnit.SECONDS));
            assertEquals(2, maxActiveCalls.get());
            releaseResponses.countDown();
            assertEquals("", first.get(2, TimeUnit.SECONDS));
            assertEquals("", second.get(2, TimeUnit.SECONDS));
            assertTrue(requestJson.get().contains("\"stream\":true"));
            assertFalse(Boolean.TRUE.equals(request.getStream()));
        } finally {
            releaseResponses.countDown();
            HermesOkHttpClientFactory.shutdown(external);
        }
    }

    @Test
    void shouldKeepFiftyConcurrentStreamsIndependent() throws Exception {
        int concurrency = 50;
        CountDownLatch allStarted = new CountDownLatch(concurrency);
        CountDownLatch releaseResponses = new CountDownLatch(1);
        AtomicInteger activeCalls = new AtomicInteger();
        AtomicInteger maxActiveCalls = new AtomicInteger();
        OkHttpClient external = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    int active = activeCalls.incrementAndGet();
                    maxActiveCalls.accumulateAndGet(active, Math::max);
                    allStarted.countDown();
                    try {
                        if (!releaseResponses.await(3, TimeUnit.SECONDS)) {
                            throw new IOException("test response release timed out");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("interrupted", e);
                    } finally {
                        activeCalls.decrementAndGet();
                    }
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "text/event-stream")
                            .body(ResponseBody.create("data: [DONE]\n\n", MediaType.get("text/event-stream")))
                            .build();
                })
                .build();
        HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
        httpConfig.setStartupCheckEnabled(false);
        HermesCliConfig cliConfig = new HermesCliConfig();
        cliConfig.setEnabled(false);
        ChatRequest request = new ChatRequest();
        request.setModel("hermes-agent");
        request.setMessages(List.of(new ChatRequest.Message("user", "ping")));

        try (HermesClient client = new HermesClient(httpConfig, cliConfig, new ObjectMapper(), external)) {
            List<StreamingChatResponse> streams = new ArrayList<>(concurrency);
            for (int index = 0; index < concurrency; index++) {
                streams.add(client.chatCompletionStream(request));
            }
            assertTrue(allStarted.await(2, TimeUnit.SECONDS));
            assertEquals(concurrency, maxActiveCalls.get());
            releaseResponses.countDown();
            for (StreamingChatResponse stream : streams) {
                assertEquals("", stream.get(2, TimeUnit.SECONDS));
            }
        } finally {
            releaseResponses.countDown();
            HermesOkHttpClientFactory.shutdown(external);
        }
    }
}
