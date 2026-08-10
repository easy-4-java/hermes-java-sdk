package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.HermesOkHttpClientFactory;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.sse.SseEvent;
import io.github.easy4j.hermes.api.sse.SseQueueSubscription;
import io.github.easy4j.hermes.api.sse.SseSubscription;
import io.github.easy4j.hermes.exception.HermesHttpException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.github.easy4j.hermes.api.HermesApiConstants.AUTH_BEARER_PREFIX;
import static io.github.easy4j.hermes.api.HermesApiConstants.CACHE_NO_CACHE;
import static io.github.easy4j.hermes.api.HermesApiConstants.CONTENT_TYPE_JSON;
import static io.github.easy4j.hermes.api.HermesApiConstants.HEADER_ACCEPT;
import static io.github.easy4j.hermes.api.HermesApiConstants.HEADER_AUTHORIZATION;
import static io.github.easy4j.hermes.api.HermesApiConstants.HEADER_CACHE_CONTROL;
import static io.github.easy4j.hermes.api.HermesApiConstants.HEADER_CONTENT_TYPE;
import static io.github.easy4j.hermes.api.HermesApiConstants.MEDIA_TYPE_SSE;
import static io.github.easy4j.hermes.api.HermesApiConstants.PATH_CHAT_COMPLETIONS;
import static io.github.easy4j.hermes.api.HermesApiConstants.PATH_RUNS;
import static io.github.easy4j.hermes.api.HermesApiConstants.PATH_SESSIONS;
import static io.github.easy4j.hermes.api.HermesApiConstants.SSE_DONE_MARKER;

/**
 * Hermes Server SSE 客户端。
 *
 * <p>所有连接均使用 OkHttp EventSource 回调；断线重连由共享调度器执行，禁止休眠循环。</p>
 */
@Slf4j
public class HermesSseClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final HermesHttpClientConfig config;
    private final ObjectMapper mapper;
    private final OkHttpClient httpClient;
    private final boolean ownsHttpClient;
    private final boolean ownsDispatcher;
    private final EventSource.Factory eventSourceFactory;
    private final Set<SubscriptionState> activeSubscriptions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconnectScheduler;

    public HermesSseClient(HermesHttpClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.ownsHttpClient = Objects.isNull(httpClient);
        this.ownsDispatcher = Objects.nonNull(httpClient);
        OkHttpClient baseClient = this.ownsHttpClient ? HermesOkHttpClientFactory.create(config) : httpClient;
        OkHttpClient.Builder clientBuilder = baseClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS);
        if (this.ownsDispatcher) {
            clientBuilder.dispatcher(HermesOkHttpClientFactory.createDispatcher(config));
        }
        this.httpClient = clientBuilder.build();
        this.eventSourceFactory = EventSources.createFactory(this.httpClient);
        AtomicInteger threadIndex = new AtomicInteger();
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable,
                    "hermes-sse-reconnect-" + threadIndex.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        log.debug("Hermes SSE client initialized: baseUrl={}, reconnectMaxAttempts={}, "
                        + "reconnectInitialDelayMs={}, reconnectMaxDelayMs={}, eventQueueCapacity={}, "
                        + "detailedLoggingEnabled={}",
                config.getBaseUrl(), config.getStreamReconnectMaxAttempts(),
                config.getStreamReconnectInitialDelayMillis(), config.getStreamReconnectMaxDelayMillis(),
                config.getStreamEventQueueCapacity(), config.isDetailedLoggingEnabled());
    }

    /** 订阅流式聊天。 */
    public SseSubscription subscribeChat(ChatRequest request, Consumer<SseEvent> consumer,
                                         Runnable onComplete, Consumer<Throwable> onError) {
        return subscribeChat(request, null, consumer, onComplete, onError);
    }

    /** 订阅流式聊天，并附加业务请求头。 */
    public SseSubscription subscribeChat(ChatRequest request, Map<String, String> headers,
                                         Consumer<SseEvent> consumer, Runnable onComplete,
                                         Consumer<Throwable> onError) {
        String url = config.getBaseUrl() + PATH_CHAT_COMPLETIONS;
        return start(() -> buildPostSseRequest(url, request, headers), consumer,
                onComplete, onError, false, "chat");
    }

    /** 订阅运行事件，断线后有界重连。 */
    public SseSubscription subscribeRunEvents(String runId, Consumer<SseEvent> consumer) {
        String url = config.getBaseUrl() + PATH_RUNS + "/" + runId + "/events";
        return start(() -> buildGetSseRequest(url), consumer, () -> { },
                error -> log.warn("Hermes run SSE stopped: runId={}, error={}", runId, error.getMessage()),
                true, "run:" + runId);
    }

    /** 创建有界队列订阅。 */
    public SseQueueSubscription subscribeRunEventsQueue(String runId) {
        BlockingQueue<SseEvent> queue = new ArrayBlockingQueue<>(
                Math.max(1, config.getStreamEventQueueCapacity()));
        SseSubscription subscription = subscribeRunEvents(runId, event -> offerLatest(queue, event));
        return new SseQueueSubscription(queue, subscription);
    }

    /** 订阅 session/chat/stream，断线后有界重连。 */
    public SseSubscription subscribeSessionEvents(String sessionId, String input,
                                                  Consumer<SseEvent> consumer) {
        String url = config.getBaseUrl() + PATH_SESSIONS + "/" + sessionId + "/chat/stream";
        return start(() -> buildPostSseRequest(url, Collections.singletonMap("input", input), null),
                consumer, () -> { },
                error -> log.warn("Hermes session SSE stopped: sessionId={}, error={}",
                        sessionId, error.getMessage()), true, "session:" + sessionId);
    }

    private SseSubscription start(RequestFactory requestFactory, Consumer<SseEvent> consumer,
                                  Runnable onComplete, Consumer<Throwable> onError,
                                  boolean reconnect, String label) {
        Objects.requireNonNull(consumer, "consumer");
        SubscriptionState subscription = new SubscriptionState();
        activeSubscriptions.add(subscription);
        connect(subscription, requestFactory, consumer, onComplete, onError, reconnect, label);
        return subscription.handle;
    }

    private void connect(SubscriptionState subscription, RequestFactory requestFactory,
                         Consumer<SseEvent> consumer, Runnable onComplete,
                         Consumer<Throwable> onError, boolean reconnect, String label) {
        if (!subscription.handle.isActive()) {
            finish(subscription);
            return;
        }
        try {
            Request request = requestFactory.create();
            long startedAt = System.nanoTime();
            AtomicBoolean terminalSignal = new AtomicBoolean();
            EventSource source = eventSourceFactory.newEventSource(request, new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    log.info("Hermes SSE connected: label={}, url={}, status={}, elapsedMs={}",
                            label, request.url(), response.code(), elapsedMillis(startedAt));
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if (SSE_DONE_MARKER.equals(data)) {
                        terminalSignal.set(true);
                        onComplete.run();
                        finish(subscription);
                        return;
                    }
                    if (data == null || data.isEmpty()) {
                        return;
                    }
                    try {
                        SseEvent event = mapper.readValue(data, SseEvent.class);
                        event.setEvent(type);
                        consumer.accept(event);
                    } catch (Exception error) {
                        log.debug("Hermes SSE parse failed: label={}, data={}", label, data, error);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    if (!terminalSignal.compareAndSet(false, true)) {
                        return;
                    }
                    if (!subscription.handle.isActive()) {
                        finish(subscription);
                        return;
                    }
                    if (reconnect && subscription.handle.isActive()) {
                        scheduleReconnect(subscription, requestFactory, consumer, onComplete,
                                onError, label, new IOException("SSE stream closed"));
                    } else {
                        onComplete.run();
                        finish(subscription);
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable error, Response response) {
                    if (!terminalSignal.compareAndSet(false, true)) {
                        return;
                    }
                    if (!subscription.handle.isActive()) {
                        finish(subscription);
                        return;
                    }
                    Throwable failure = Objects.nonNull(error) ? error
                            : new HermesHttpException(Objects.nonNull(response) ? response.code() : -1, "");
                    if (reconnect && subscription.handle.isActive()) {
                        scheduleReconnect(subscription, requestFactory, consumer, onComplete,
                                onError, label, failure);
                    } else {
                        onError.accept(failure);
                        finish(subscription);
                    }
                }
            });
            subscription.sourceRef.set(source);
            if (!subscription.handle.isActive()) {
                source.cancel();
            }
        } catch (Exception error) {
            if (reconnect && subscription.handle.isActive()) {
                scheduleReconnect(subscription, requestFactory, consumer, onComplete,
                        onError, label, error);
            } else {
                onError.accept(error);
                finish(subscription);
            }
        }
    }

    private void scheduleReconnect(SubscriptionState subscription, RequestFactory requestFactory,
                                   Consumer<SseEvent> consumer, Runnable onComplete,
                                   Consumer<Throwable> onError, String label, Throwable cause) {
        if (!subscription.handle.isActive() || reconnectScheduler.isShutdown()) {
            finish(subscription);
            return;
        }
        int attempt = subscription.reconnectAttempts.incrementAndGet();
        if (attempt > Math.max(0, config.getStreamReconnectMaxAttempts())) {
            log.warn("Hermes SSE reconnect exhausted: label={}, attempts={}, error={}",
                    label, attempt - 1, cause.getMessage());
            onError.accept(cause);
            finish(subscription);
            return;
        }
        long delay = reconnectDelayMillis(attempt);
        log.warn("Hermes SSE reconnect scheduled: label={}, attempt={}, delayMs={}, error={}",
                label, attempt, delay, cause.getMessage());
        ScheduledFuture<?> future;
        try {
            future = reconnectScheduler.schedule(
                    () -> connect(subscription, requestFactory, consumer, onComplete, onError, true, label),
                    delay, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            finish(subscription);
            return;
        }
        ScheduledFuture<?> previous = subscription.retryRef.getAndSet(future);
        if (Objects.nonNull(previous)) {
            previous.cancel(false);
        }
    }

    private long reconnectDelayMillis(int attempt) {
        long initial = Math.max(1L, config.getStreamReconnectInitialDelayMillis());
        long maximum = Math.max(initial, config.getStreamReconnectMaxDelayMillis());
        int shift = Math.min(20, Math.max(0, attempt - 1));
        long exponential = initial > Long.MAX_VALUE >> shift ? maximum : initial << shift;
        long bounded = Math.min(maximum, exponential);
        long jitterBound = Math.max(1L, bounded / 4L);
        return Math.min(maximum, bounded + ThreadLocalRandom.current().nextLong(jitterBound));
    }

    private void offerLatest(BlockingQueue<SseEvent> queue, SseEvent event) {
        if (!queue.offer(event)) {
            queue.poll();
            queue.offer(event);
            log.warn("Hermes SSE event queue is full; discarded oldest event");
        }
    }

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
        Request.Builder builder = authedBuilder(url).header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (Objects.nonNull(headers)) {
            headers.forEach((name, value) -> {
                if (Objects.nonNull(name) && Objects.nonNull(value)) {
                    builder.header(name, value);
                }
            });
        }
        return builder.post(RequestBody.create(mapper.writeValueAsString(body), JSON)).build();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private void finish(SubscriptionState subscription) {
        subscription.handle.close();
    }

    /** 返回当前活动订阅数量。 */
    public int activeSubscriptionCount() {
        return activeSubscriptions.size();
    }

    private void closeSubscriptions() {
        for (SubscriptionState subscription : activeSubscriptions) {
            subscription.handle.close();
        }
        activeSubscriptions.clear();
    }

    @Override
    public void close() {
        closeSubscriptions();
        reconnectScheduler.shutdownNow();
        if (ownsHttpClient) {
            HermesOkHttpClientFactory.shutdown(httpClient);
        } else if (ownsDispatcher) {
            httpClient.dispatcher().cancelAll();
            httpClient.dispatcher().executorService().shutdown();
        }
    }

    @FunctionalInterface
    private interface RequestFactory {
        Request create() throws IOException;
    }

    /** Hermes 重连状态；公共生命周期只通过 SseSubscription 暴露。 */
    private final class SubscriptionState {
        private final AtomicInteger reconnectAttempts = new AtomicInteger();
        private final AtomicReference<EventSource> sourceRef = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> retryRef = new AtomicReference<>();
        private final SseSubscription handle = new SseSubscription(this::cancel);

        private void cancel() {
            EventSource source = sourceRef.getAndSet(null);
            if (Objects.nonNull(source)) {
                source.cancel();
            }
            ScheduledFuture<?> retry = retryRef.getAndSet(null);
            if (Objects.nonNull(retry)) {
                retry.cancel(false);
            }
            activeSubscriptions.remove(this);
        }
    }
}
