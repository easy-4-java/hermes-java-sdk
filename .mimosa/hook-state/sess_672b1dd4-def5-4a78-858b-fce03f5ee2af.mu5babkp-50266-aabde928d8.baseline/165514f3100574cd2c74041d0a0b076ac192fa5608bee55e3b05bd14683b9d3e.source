package io.github.easy4j.hermes.api;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
import okhttp3.extension.logging.HttpLogLevel;

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
 * <p>Hermes Server SSE 客户端。</p>
 *
 * <p>使用 OkHttp EventSource 管理非阻塞事件流、有界指数退避重连、显式队列订阅和幂等取消生命周期。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesSseClient implements AutoCloseable {

    /**
     * 请求体 JSON 媒体类型，使用 UTF-8 编码。
     */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * 当前客户端使用的配置快照。
     */
    private final HermesHttpClientConfig config;
    /**
     * SSE 事件反序列化使用的 ObjectMapper。
     */
    private final ObjectMapper mapper;
    /**
     * 执行 HTTP 请求的 OkHttpClient。
     */
    private final OkHttpClient httpClient;
    /**
     * 当前对象是否负责关闭 HTTP 客户端。
     */
    private final boolean ownsHttpClient;
    /**
     * 当前对象是否负责关闭派生 Dispatcher。
     */
    private final boolean ownsDispatcher;
    /**
     * 创建 OkHttp EventSource 的工厂。
     */
    private final EventSource.Factory eventSourceFactory;
    /**
     * 当前仍处于活动状态的 SSE 订阅集合。
     */
    private final Set<SubscriptionState> activeSubscriptions = ConcurrentHashMap.newKeySet();
    /**
     * 执行 SSE 延迟重连的单线程守护调度器。
     */
    private final ScheduledExecutorService reconnectScheduler;

    /**
     * <p>创建 HermesSseClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesSseClient(HermesHttpClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = Objects.isNull(objectMapper) ? JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build() : objectMapper;
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
        debug(HttpLogLevel.BASIC, "Hermes SSE client initialized: baseUrl={}, reconnectMaxAttempts={}, "
                        + "reconnectInitialDelayMs={}, reconnectMaxDelayMs={}, eventQueueCapacity={}, debugLevel={}",
                config.getBaseUrl(), config.getStreamReconnectMaxAttempts(),
                config.getStreamReconnectInitialDelayMillis(), config.getStreamReconnectMaxDelayMillis(),
                config.getStreamEventQueueCapacity(), config.getDebug().getLevel());
    }

    /**
     * <p>订阅聊天 SSE 事件。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param consumer 事件或增量消费者，不得为 {@code null}
     * @param onComplete 流正常结束时执行的回调
     * @param onError 流失败时接收异常的回调
     * @return 可用于取消和关闭底层连接的订阅句柄
     * @since 1.0.0
     */
    public SseSubscription subscribeChat(ChatRequest request, Consumer<SseEvent> consumer,
                                         Runnable onComplete, Consumer<Throwable> onError) {
        return subscribeChat(request, null, consumer, onComplete, onError);
    }

    /**
     * <p>订阅聊天 SSE 事件。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @param consumer 事件或增量消费者，不得为 {@code null}
     * @param onComplete 流正常结束时执行的回调
     * @param onError 流失败时接收异常的回调
     * @return 可用于取消和关闭底层连接的订阅句柄
     * @since 1.0.0
     */
    public SseSubscription subscribeChat(ChatRequest request, Map<String, String> headers,
                                         Consumer<SseEvent> consumer, Runnable onComplete,
                                         Consumer<Throwable> onError) {
        String url = config.getBaseUrl() + PATH_CHAT_COMPLETIONS;
        return start(() -> buildPostSseRequest(url, request, headers), consumer,
                onComplete, onError, false, "chat");
    }

    /**
     * <p>订阅 Run SSE 事件。</p>
     *
     * @param runId Run 唯一标识
     * @param consumer 事件或增量消费者，不得为 {@code null}
     * @return 可用于取消和关闭底层连接的订阅句柄
     * @since 1.0.0
     */
    public SseSubscription subscribeRunEvents(String runId, Consumer<SseEvent> consumer) {
        String url = config.getBaseUrl() + PATH_RUNS + "/" + runId + "/events";
        return start(() -> buildGetSseRequest(url), consumer, () -> { },
                error -> log.warn("Hermes run SSE stopped: runId={}, error={}", runId, error.getMessage()),
                true, "run:" + runId);
    }

    /**
     * <p>创建 Run 的有界队列订阅。</p>
     *
     * <p>仅该显式 API 创建事件队列；队列已满时丢弃最旧事件，避免无界内存增长。</p>
     *
     * @param runId Run 唯一标识
     * @return 包含有界队列与关闭句柄的订阅对象
     * @since 1.0.0
     */
    public SseQueueSubscription subscribeRunEventsQueue(String runId) {
        BlockingQueue<SseEvent> queue = new ArrayBlockingQueue<>(
                Math.max(1, config.getStreamEventQueueCapacity()));
        SseSubscription subscription = subscribeRunEvents(runId, event -> offerLatest(queue, event));
        return new SseQueueSubscription(queue, subscription);
    }

    /**
     * <p>订阅会话聊天 SSE 事件。</p>
     *
     * @param sessionId 会话唯一标识
     * @param input 用户输入文本
     * @param consumer 事件或增量消费者，不得为 {@code null}
     * @return 可用于取消和关闭底层连接的订阅句柄
     * @since 1.0.0
     */
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
            // OkHttp 可能先后触发 failure 和 closed；该标记保证完成、失败或重连只选择一次。
            AtomicBoolean terminalSignal = new AtomicBoolean();
            EventSource source = eventSourceFactory.newEventSource(request, new EventSourceListener() {
                /**
                 * <p>处理 SSE 连接建立回调。</p>
                 *
                 * @param eventSource 产生回调的 EventSource
                 * @param response OkHttp 响应
                 * @since 1.0.0
                 */
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    debug(HttpLogLevel.BASIC, "Hermes SSE connected: label={}, url={}, status={}, elapsedMs={}",
                            label, request.url(), response.code(), elapsedMillis(startedAt));
                }

                /**
                 * <p>处理 SSE 事件回调。</p>
                 *
                 * @param eventSource 产生回调的 EventSource
                 * @param id 对象唯一标识
                 * @param type 事件类型或目标 Java 类型
                 * @param data SSE 原始数据
                 * @since 1.0.0
                 */
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
                        if (config.getDebug().allows(HttpLogLevel.BODY)) {
                            log.debug("Hermes SSE parse failed: label={}, data={}", label, truncate(data), error);
                        } else {
                            debug(HttpLogLevel.BASIC, "Hermes SSE parse failed: label={}, dataLength={}, error={}",
                                    label, data.length(), error.getMessage());
                        }
                    }
                }

                /**
                 * <p>处理 SSE 连接关闭回调。</p>
                 *
                 * @param eventSource 产生回调的 EventSource
                 * @since 1.0.0
                 */
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

                /**
                 * <p>处理异步传输失败回调。</p>
                 *
                 * @param eventSource 产生回调的 EventSource
                 * @param error 导致流失败的异常
                 * @param response OkHttp 响应
                 * @since 1.0.0
                 */
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
        // 共享调度器执行延迟重连；指数退避与随机抖动避免大量连接同步冲击服务端。
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
            // 容量耗尽时淘汰最旧事件并保留最新状态，确保事件缓存具有固定内存上限。
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

    private void debug(HttpLogLevel level, String message, Object... arguments) {
        if (config.getDebug().allows(level)) {
            log.debug(message, arguments);
        }
    }

    private String truncate(String value) {
        int limit = config.getDebug().resolveMaxContentLength();
        return value.length() <= limit ? value : value.substring(0, limit) + "...<truncated>";
    }

    private void finish(SubscriptionState subscription) {
        subscription.handle.close();
    }

    /**
     * <p>返回当前活动 SSE 订阅数量。</p>
     *
     * @return 当前活动且尚未关闭的 SSE 订阅数量
     * @since 1.0.0
     */
    public int activeSubscriptionCount() {
        return activeSubscriptions.size();
    }

    private void closeSubscriptions() {
        for (SubscriptionState subscription : activeSubscriptions) {
            subscription.handle.close();
        }
        activeSubscriptions.clear();
    }

    /**
     * <p>关闭当前对象并释放其拥有的资源。</p>
     *
     * <p>重复关闭不会重复释放资源；调用方注入且不归当前对象所有的共享资源不会被关闭。</p>
     *
     * @since 1.0.0
     */
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

    /**
     * <p>SSE 请求工厂。</p>
     *
     * <p>为首次连接和每次重连创建新的 OkHttp Request。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    @FunctionalInterface
    private interface RequestFactory {
        Request create() throws IOException;
    }

    /**
     * <p>SSE 订阅重连状态。</p>
     *
     * <p>原子保存当前 EventSource、待执行重连任务和重试次数，对外仅暴露订阅句柄。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    private final class SubscriptionState {
        /**
         * 当前连续重连尝试次数。
         */
        private final AtomicInteger reconnectAttempts = new AtomicInteger();
        /**
         * 当前 EventSource 的原子引用。
         */
        private final AtomicReference<EventSource> sourceRef = new AtomicReference<>();
        /**
         * 待执行重连任务的原子引用。
         */
        private final AtomicReference<ScheduledFuture<?>> retryRef = new AtomicReference<>();
        /**
         * 对外暴露的幂等订阅句柄。
         */
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
