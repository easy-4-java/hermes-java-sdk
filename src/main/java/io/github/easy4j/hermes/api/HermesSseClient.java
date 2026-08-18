package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesClientConfig;
import static io.github.easy4j.hermes.api.HermesApiConstants.*;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.SseEvent;
import io.github.easy4j.hermes.exception.HermesHttpException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Hermes Server SSE 客户端，消费 run 事件流和 session/chat 流式聊天。
 * <p>基于 OkHttp，支持外部传入 {@link OkHttpClient}。
 * GET SSE 用 {@code okhttp3.sse.EventSources}；POST SSE 用 {@code Response.body().source()} 手动解析。</p>
 */
@Slf4j
public class HermesSseClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final HermesClientConfig config;
    private final ObjectMapper mapper;
    private final OkHttpClient httpClient;
    private final AtomicReference<Subscription> activeSubscription = new AtomicReference<>();

    public HermesSseClient(HermesClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.httpClient = Objects.isNull(httpClient) ? buildSseOkHttpClient(config) : httpClient;
    }

    private static OkHttpClient buildSseOkHttpClient(HermesClientConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS); // SSE 无读超时
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
    }

    // ============================================================
    // Chat Completion SSE（POST → SSE 流式）
    // ============================================================

    public void subscribeChat(ChatRequest request,
                              Consumer<SseEvent> consumer,
                              Runnable onComplete,
                              Consumer<Throwable> onError) {
        subscribeChat(request, null, consumer, onComplete, onError);
    }

    public void subscribeChat(ChatRequest request,
                              Map<String, String> headers,
                              Consumer<SseEvent> consumer,
                              Runnable onComplete,
                              Consumer<Throwable> onError) {
        stopCurrent();
        Subscription sub = new Subscription("hermes-sse-chat");
        activeSubscription.set(sub);
        sub.executor.submit(() -> doSubscribePost(
                config.getServerUrl() + PATH_CHAT_COMPLETIONS,
                request, headers, consumer, onComplete, onError, sub));
    }

    // ============================================================
    // Run Events SSE（GET → SSE 事件流，带断线重连）
    // ============================================================

    public void subscribe(String runId, Consumer<SseEvent> consumer) {
        stopCurrent();
        Subscription sub = new Subscription("hermes-sse-run");
        activeSubscription.set(sub);
        sub.executor.submit(() -> doSubscribeRun(runId, consumer, sub));
    }

    public BlockingQueue<SseEvent> subscribeQueue(String runId) {
        BlockingQueue<SseEvent> queue = new LinkedBlockingQueue<>();
        subscribe(runId, queue::offer);
        return queue;
    }

    // ============================================================
    // Session Stream SSE（POST → SSE 流式，带断线重连）
    // ============================================================

    public void subscribeSessionStream(String sessionId, String input, Consumer<SseEvent> consumer) {
        stopCurrent();
        Subscription sub = new Subscription("hermes-sse-session");
        activeSubscription.set(sub);
        sub.executor.submit(() -> doSubscribeSessionStream(sessionId, input, consumer, sub));
    }

    // ============================================================
    // Internals — POST SSE（手动解析 BufferedSource）
    // ============================================================

    private void doSubscribePost(String url,
                                 Object requestBody,
                                 Map<String, String> headers,
                                 Consumer<SseEvent> consumer,
                                 Runnable onComplete,
                                 Consumer<Throwable> onError,
                                 Subscription sub) {
        try {
            Request request = buildPostSseRequest(url, requestBody, headers);
            sub.callRef.set(httpClient.newCall(request));
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    onError.accept(new HermesHttpException(response.code(), body));
                    return;
                }
                if (response.body() != null) {
                    parseSseSource(response.body().source(), consumer, onComplete, sub);
                }
            }
            onComplete.run();
        } catch (Exception e) {
            if (sub.running) {
                log.warn("SSE chat error", e);
                onError.accept(e);
            }
        } finally {
            sub.executor.shutdown();
        }
    }

    private void doSubscribeSessionStream(String sessionId, String input,
                                          Consumer<SseEvent> consumer, Subscription sub) {
        while (sub.running) {
            try {
                String url = config.getServerUrl() + PATH_SESSIONS + "/" + sessionId + "/chat/stream";
                Request request = buildPostSseRequest(url, Collections.singletonMap("input", input), null);
                sub.callRef.set(httpClient.newCall(request));
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        log.warn("SSE session stream failed status={}, retrying", response.code());
                        Thread.sleep(DEFAULT_CONNECT_TIMEOUT_MS / 3);
                        continue;
                    }
                    if (response.body() != null) {
                        parseSseSource(response.body().source(), consumer, () -> {}, sub);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (sub.running) {
                    log.warn("SSE session stream lost, retrying", e);
                    try { Thread.sleep(DEFAULT_CONNECT_TIMEOUT_MS / 3); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
    }

    private void doSubscribeRun(String runId, Consumer<SseEvent> consumer, Subscription sub) {
        while (sub.running) {
            try {
                String url = config.getServerUrl() + PATH_RUNS + "/" + runId + "/events";
                Request request = buildGetSseRequest(url);
                sub.callRef.set(httpClient.newCall(request));
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("SSE run events failed status={}, retrying", response.code());
                        Thread.sleep(DEFAULT_CONNECT_TIMEOUT_MS / 3);
                        continue;
                    }
                    log.info("SSE connected to run events {}", runId);
                    if (response.body() != null) {
                        parseSseSource(response.body().source(), consumer, () -> {}, sub);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (sub.running) {
                    log.warn("SSE connection lost, retrying", e);
                    try { Thread.sleep(DEFAULT_CONNECT_TIMEOUT_MS / 3); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
    }

    // ============================================================
    // SSE 解析（从 OkHttp BufferedSource 逐行读）
    // ============================================================

    private void parseSseSource(okio.BufferedSource source,
                                Consumer<SseEvent> consumer,
                                Runnable onComplete,
                                Subscription sub) throws IOException {
        String currentEvent = null;
        while (sub.running && !source.exhausted()) {
            String line = source.readUtf8Line();
            if (line == null) break;
            if (line.isEmpty()) { currentEvent = null; continue; }
            if (line.startsWith(SSE_EVENT_PREFIX)) {
                currentEvent = line.substring(SSE_EVENT_PREFIX.length()).trim();
                continue;
            }
            if (line.startsWith(SSE_DATA_PREFIX)) {
                String json = line.substring(SSE_DATA_PREFIX.length()).trim();
                if (SSE_DONE_MARKER.equals(json)) { onComplete.run(); return; }
                if (!json.isEmpty()) {
                    try {
                        SseEvent event = mapper.readValue(json, SseEvent.class);
                        event.setEvent(currentEvent);
                        consumer.accept(event);
                    } catch (Exception e) { log.debug("SSE parse: {}", json, e); }
                }
            }
        }
    }

    // ============================================================
    // Request builders
    // ============================================================

    private Request.Builder authedBuilder(String url) {
        Request.Builder builder = new Request.Builder().url(url)
                .header(HEADER_ACCEPT, MEDIA_TYPE_SSE)
                .header(HEADER_CACHE_CONTROL, CACHE_NO_CACHE);
        String apiKey = config.resolveApiKey();
        if (!apiKey.isEmpty()) {
            builder.header(HEADER_AUTHORIZATION, AUTH_BEARER_PREFIX + apiKey);
        }
        return builder;
    }

    private Request buildGetSseRequest(String url) {
        return authedBuilder(url).get().build();
    }

    private Request buildPostSseRequest(String url, Object body, Map<String, String> headers) throws IOException {
        Request.Builder builder = authedBuilder(url)
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (headers != null) {
            headers.forEach((k, v) -> { if (k != null && v != null) builder.header(k, v); });
        }
        return builder.post(RequestBody.create(mapper.writeValueAsString(body), JSON)).build();
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    public void stop() { stopCurrent(); }

    private void stopCurrent() {
        Subscription old = activeSubscription.getAndSet(null);
        if (old != null) old.stop();
    }

    @Override
    public void close() { stop(); }

    private static class Subscription {
        volatile boolean running = true;
        final AtomicReference<Call> callRef = new AtomicReference<>();
        final ExecutorService executor;

        Subscription(String name) {
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, name);
                t.setDaemon(true);
                return t;
            });
        }

        void stop() {
            running = false;
            Call call = callRef.get();
            if (call != null) call.cancel();
            executor.shutdownNow();
        }
    }
}
