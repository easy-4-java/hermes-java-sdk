package io.github.easy4j.hermes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.api.model.*;
import io.github.easy4j.hermes.cli.HermesCli;
import io.github.easy4j.hermes.cli.HermesCliExecutor;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.sse.SseSubscription;
import io.github.easy4j.hermes.api.sse.StreamingChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * <p>Hermes SDK 统一客户端门面。</p>
 *
 * <p>统一管理 HTTP、SSE 与本地 CLI 通道，并负责共享传输、托管 profile 视图和资源关闭生命周期。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesClient implements AutoCloseable {

    /**
     * 限制 Gateway profile 标识为 1 至 128 个安全字符的校验表达式。
     */
    private static final Pattern PROFILE_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    /**
     * 当前客户端使用的配置快照。
     */
    private final HermesClientConfig config;
    /**
     * 执行 HTTP 请求的 OkHttpClient。
     */
    private final HermesHttpClient httpClient;
    /**
     * 聊天场景客户端。
     */
    private final HermesChatClient chatClient;
    /**
     * SSE 事件流客户端。
     */
    private final HermesSseClient sseClient;
    /**
     * 本地 Hermes CLI 客户端。
     */
    private final HermesCli cli;
    /**
     * 仅在 SDK 自建传输时保存、并由根客户端关闭的 OkHttpClient。
     */
    private final OkHttpClient ownedHttpClient;
    /**
     * JSON 序列化与反序列化使用的 ObjectMapper。
     */
    private final ObjectMapper objectMapper;
    /**
     * 根客户端及 profile 视图共享的 OkHttpClient。
     */
    private final OkHttpClient sharedHttpClient;
    /**
     * 当前实例是否为根客户端管理的 profile 视图。
     */
    private final boolean managedProfileView;
    /**
     * 按 profile 标识缓存的托管客户端视图。
     */
    private final ConcurrentMap<String, HermesClient> profileClients = new ConcurrentHashMap<>();
    /**
     * 根客户端是否已经关闭的原子标记。
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesClient(HermesClientConfig config) {
        this(Objects.requireNonNull(config, "config").getHttp(), config.getCli(), new ObjectMapper(),
                HermesOkHttpClientFactory.create(config.getHttp()), true);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesClientConfig config, OkHttpClient httpClient) {
        this(config, new ObjectMapper(), httpClient);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(Objects.requireNonNull(config, "config").getHttp(),
                config.getCli(),
                objectMapper,
                httpClient,
                false);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param httpConfig HTTP 配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesClient(HermesHttpClientConfig httpConfig) {
        this(httpConfig, new HermesCliConfig());
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param httpConfig HTTP 配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesHttpClientConfig httpConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(httpConfig, new HermesCliConfig(), objectMapper, httpClient);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param cliConfig CLI 配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesClient(HermesCliConfig cliConfig) {
        this(new HermesHttpClientConfig(), cliConfig);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param cliConfig CLI 配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesCliConfig cliConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(new HermesHttpClientConfig(), cliConfig, objectMapper, httpClient);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param httpConfig HTTP 配置，不得为 {@code null}
     * @param cliConfig CLI 配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig) {
        this(httpConfig, cliConfig, new ObjectMapper(), HermesOkHttpClientFactory.create(httpConfig), true);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param httpConfig HTTP 配置，不得为 {@code null}
     * @param cliConfig CLI 配置，不得为 {@code null}
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig,
                        OkHttpClient httpClient) {
        this(httpConfig, cliConfig, new ObjectMapper(), httpClient);
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param httpConfig HTTP 配置，不得为 {@code null}
     * @param cliConfig CLI 配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig,
                        ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(httpConfig, cliConfig, objectMapper, httpClient, false);
    }

    private HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig,
                         ObjectMapper objectMapper, OkHttpClient httpClient, boolean ownsHttpClient) {
        this(httpConfig, cliConfig, objectMapper, httpClient, ownsHttpClient, false);
    }

    private HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig,
                         ObjectMapper objectMapper, OkHttpClient httpClient, boolean ownsHttpClient,
                         boolean managedProfileView) {
        Objects.requireNonNull(httpConfig, "httpConfig");
        Objects.requireNonNull(cliConfig, "cliConfig");
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(httpClient, "httpClient");
        // 根客户端只关闭 SDK 自建传输；外部注入客户端及其连接池始终由调用方管理。
        // profile 视图复用同一传输，不能取得共享资源的所有权。
        this.ownedHttpClient = ownsHttpClient ? httpClient : null;
        this.objectMapper = objectMapper;
        this.sharedHttpClient = httpClient;
        this.managedProfileView = managedProfileView;
        this.config = new HermesClientConfig();
        // 保存配置快照，避免调用方后续修改原 POJO 导致门面与底层客户端行为分叉。
        copyHttpConfig(httpConfig);
        copyCliConfig(cliConfig);

        // HTTP、Chat 和 SSE 共享 OkHttpClient 与 ObjectMapper；初始化过程不创建每请求线程。
        if (httpConfig.isEnabled()) {
            this.sseClient = new HermesSseClient(httpConfig, objectMapper, httpClient);
            this.chatClient = new HermesChatClient(
                    httpConfig, objectMapper, httpClient, this.sseClient);
            this.httpClient = this.chatClient;
            // 启动检查是显式可选的同步探测；失败是否终止初始化由 fail-fast 配置决定。
            if (httpConfig.isStartupCheckEnabled()) {
                try {
                    HealthStatus status = this.httpClient.health();
                    if (!"ok".equals(status.getStatus())) {
                        handleHttpCheckFailed(httpConfig, "Health check failed: " + status.getStatus());
                    } else {
                        log.info("Hermes HTTP health check passed: baseUrl={}, defaultModel={}",
                                httpConfig.getBaseUrl(), httpConfig.getDefaultModel());
                    }
                } catch (Exception e) {
                    handleHttpCheckFailed(httpConfig, "Health check failed: " + e.getMessage());
                }
            }
        } else {
            this.httpClient = null;
            this.chatClient = null;
            this.sseClient = null;
        }

        // CLI 是独立通道，启停与 HTTP/SSE 生命周期互不绑定。
        if (cliConfig.isEnabled()) {
            HermesCliExecutor executor = new HermesCliExecutor(cliConfig);
            boolean cliAvailable = !cliConfig.isStartupCheckEnabled() || executor.probe();
            if (!cliAvailable) {
                handleCliCheckFailed(cliConfig);
            }
            this.cli = new HermesCli(executor);
        } else {
            this.cli = null;
        }
    }

    /**
     * <p>创建 HermesClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @param sseClient SSE 客户端
     * @param cli 本地 CLI 客户端
     * @since 1.0.0
     */
    public HermesClient(HermesClientConfig config, HermesHttpClient httpClient,
                        HermesSseClient sseClient, HermesCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.chatClient = httpClient instanceof HermesChatClient ? (HermesChatClient) httpClient : null;
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.cli = Objects.requireNonNull(cli, "cli");
        this.ownedHttpClient = null;
        this.objectMapper = new ObjectMapper();
        this.sharedHttpClient = httpClient.getOkHttpClient();
        this.managedProfileView = false;
    }

    private void copyHttpConfig(HermesHttpClientConfig src) {
        copyHttpConfig(src, this.config.getHttp());
    }

    private static void copyHttpConfig(HermesHttpClientConfig src, HermesHttpClientConfig target) {
        target.setMode(src.getMode());
        target.setEnabled(src.isEnabled());
        target.setStartupCheckEnabled(src.isStartupCheckEnabled());
        target.setFailFastOnUnavailable(src.isFailFastOnUnavailable());
        target.setBaseUrl(src.getBaseUrl());
        target.setApiKey(src.getApiKey());
        target.setConnectTimeoutMillis(src.getConnectTimeoutMillis());
        target.setReadTimeoutMillis(src.getReadTimeoutMillis());
        target.setWriteTimeoutMillis(src.getWriteTimeoutMillis());
        target.setCallTimeoutMillis(src.getCallTimeoutMillis());
        target.setMaxIdleConnections(src.getMaxIdleConnections());
        target.setKeepAliveDurationMillis(src.getKeepAliveDurationMillis());
        target.setMaxRequests(src.getMaxRequests());
        target.setMaxRequestsPerHost(src.getMaxRequestsPerHost());
        target.setStreamCorePoolSize(src.getStreamCorePoolSize());
        target.setStreamMaxPoolSize(src.getStreamMaxPoolSize());
        target.setStreamQueueCapacity(src.getStreamQueueCapacity());
        target.setStreamKeepAliveMillis(src.getStreamKeepAliveMillis());
        target.setStreamEventQueueCapacity(src.getStreamEventQueueCapacity());
        target.setStreamReconnectMaxAttempts(src.getStreamReconnectMaxAttempts());
        target.setStreamReconnectInitialDelayMillis(src.getStreamReconnectInitialDelayMillis());
        target.setStreamReconnectMaxDelayMillis(src.getStreamReconnectMaxDelayMillis());
        target.setRetryOnConnectionFailure(src.isRetryOnConnectionFailure());
        target.getDebug().setEnabled(src.getDebug().isEnabled());
        target.getDebug().setLevel(src.getDebug().getLevel());
        target.getDebug().setMaxContentLength(src.getDebug().getMaxContentLength());
        target.setVerifySsl(src.isVerifySsl());
        target.setDefaultModel(src.getDefaultModel());
        target.setDefaultInstructions(src.getDefaultInstructions());
        target.setDefaultProvider(src.getDefaultProvider());
    }

    private void copyCliConfig(HermesCliConfig src) {
        this.config.getCli().setEnabled(src.isEnabled());
        this.config.getCli().setStartupCheckEnabled(src.isStartupCheckEnabled());
        this.config.getCli().setFailFastOnUnavailable(src.isFailFastOnUnavailable());
        this.config.getCli().setExecutable(src.getExecutable());
        this.config.getCli().setTimeout(src.getTimeout());
        this.config.getCli().setProbeTimeoutSeconds(src.getProbeTimeoutSeconds());
        this.config.getCli().setWorkingDirectory(src.getWorkingDirectory());
        this.config.getCli().setMaxConcurrentExecutions(src.getMaxConcurrentExecutions());
    }

    private void handleHttpCheckFailed(HermesHttpClientConfig config, String message) {
        if (config.isFailFastOnUnavailable()) {
            throw new IllegalStateException("Hermes HTTP service is not available: " + message
                    + ". Set HermesHttpClientConfig.enabled=false or startupCheckEnabled=false to disable.");
        }
        log.warn("Hermes HTTP service is not available: {} (continuing without HTTP support)", message);
    }

    private void handleCliCheckFailed(HermesCliConfig config) {
        if (config.isFailFastOnUnavailable()) {
            throw new IllegalStateException("Hermes CLI is not available: " + config.getExecutable()
                    + ". Set HermesCliConfig.enabled=false or startupCheckEnabled=false to disable.");
        }
        log.warn("Hermes CLI is not available: {} (continuing without CLI support)", config.getExecutable());
    }

    // ============================================================
    // Status checks
    // ============================================================

    /**
     * <p>判断 HTTP 通道是否启用。</p>
     *
     * @return HTTP、Chat 与 SSE 客户端均已初始化时返回 {@code true}
     * @since 1.0.0
     */
    public boolean isHttpEnabled() { return httpClient != null; }
    /**
     * <p>判断 CLI 通道是否启用。</p>
     *
     * @return 本地 CLI 客户端已初始化时返回 {@code true}
     * @since 1.0.0
     */
    public boolean isCliEnabled() { return cli != null; }

    // ============================================================
    // Health
    // ============================================================

    /**
     * <p>查询 Hermes 基础健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus health() { return httpClient.health(); }
    /**
     * <p>异步查询 Hermes 基础健康状态。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @return 异步承载服务健康状态的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<HealthStatus> healthAsync() { return httpClient.healthAsync(); }
    /**
     * <p>查询 Hermes 详细健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus healthDetailed() { return httpClient.healthDetailed(); }
    /**
     * <p>查询 Hermes V1 健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus healthV1() { return httpClient.healthV1(); }

    // ============================================================
    // Chat Completions
    // ============================================================

    /**
     * <p>执行聊天补全请求。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletion(ChatRequest request) {
       
        return httpClient.chatCompletion(request);
    }

    /**
     * <p>异步执行聊天补全请求。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return 异步承载类型化聊天补全响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ChatResponse> chatCompletionAsync(ChatRequest request) {
        return httpClient.chatCompletionAsync(request);
    }

    /**
     * <p>执行聊天补全请求。</p>
     *
     * <p>在基础鉴权请求头之外附加非空业务请求头；同步入口会等待底层请求完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletion(ChatRequest request,
                                       Map<String, String> headers) {
       
        return httpClient.chatCompletion(request, headers);
    }

    /**
     * <p>异步执行聊天补全请求。</p>
     *
     * <p>附加非空业务请求头并通过 OkHttp enqueue 非阻塞提交；失败通过 CompletableFuture 异常完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return 异步承载类型化聊天补全响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ChatResponse> chatCompletionAsync(ChatRequest request,
                                                               Map<String, String> headers) {
        return httpClient.chatCompletionAsync(request, headers, null);
    }

    /**
     * <p>在指定 Hermes 会话上下文中执行聊天补全请求。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request,
                                                  String sessionKey,
                                                  String sessionId) {
       
        return httpClient.chatCompletion(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null));
    }

    /**
     * <p>在指定 Hermes 会话上下文中执行聊天补全请求。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionWithSession(request, sessionKey, (String) null);
    }

    /**
     * <p>在指定 Hermes 会话上下文中执行聊天补全请求。</p>
     *
     * <p>业务取消信号会传播到底层 OkHttp Call，避免已无消费者的请求继续占用连接。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param cancellation 调用取消信号；可以为 {@code null}
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return httpClient.chatCompletion(request,
                HermesHttpClient.hermesHeaders(sessionKey, null, null), cancellation);
    }

    // ============================================================
    // Responses API
    // ============================================================

    /**
     * <p>创建 Responses API 响应。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return Responses API 响应
     * @since 1.0.0
     */
    public ResponseResult createResponse(ResponseRequest request) {
       
        return httpClient.createResponse(request);
    }

    /**
     * <p>异步创建 Responses API 响应。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return 异步承载 Responses API 响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ResponseResult> createResponseAsync(ResponseRequest request) {
        return httpClient.createResponseAsync(request);
    }

    /**
     * <p>创建 Responses API 响应。</p>
     *
     * <p>在基础鉴权请求头之外附加非空业务请求头；同步入口会等待底层请求完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return Responses API 响应
     * @since 1.0.0
     */
    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
       
        return httpClient.createResponse(request, headers);
    }

    /**
     * <p>异步创建 Responses API 响应。</p>
     *
     * <p>附加非空业务请求头并通过 OkHttp enqueue 非阻塞提交；失败通过 CompletableFuture 异常完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return 异步承载 Responses API 响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ResponseResult> createResponseAsync(ResponseRequest request,
                                                                 Map<String, String> headers) {
        return httpClient.createResponseAsync(request, headers);
    }

    /**
     * <p>按标识查询 Responses API 响应。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param responseId 响应唯一标识
     * @return Responses API 响应
     * @since 1.0.0
     */
    public ResponseResult getResponse(String responseId) { return httpClient.getResponse(responseId); }
    /**
     * <p>删除指定 Responses API 响应。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param responseId 响应唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteResponse(String responseId) { return httpClient.deleteResponse(responseId); }

    // ============================================================
    // Models & Capabilities & Skills
    // ============================================================

    /**
     * <p>列出 Hermes 可用模型。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 模型列表响应
     * @since 1.0.0
     */
    public ModelsResponse listModels() { return httpClient.listModels(); }
    /**
     * <p>异步列出 Hermes 可用模型。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @return 异步承载模型列表响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ModelsResponse> listModelsAsync() { return httpClient.listModelsAsync(); }
    /**
     * <p>查询 Hermes 服务能力。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务能力信息
     * @since 1.0.0
     */
    public CapabilityInfo getCapabilities() { return httpClient.getCapabilities(); }
    /**
     * <p>列出可用技能。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 服务端已加载技能的描述列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> listSkills() { return httpClient.listSkills(); }
    /**
     * <p>列出可用工具集。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 服务端可用工具集的描述列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> listToolsets() { return httpClient.listToolsets(); }

    // ============================================================
    // Run
    // ============================================================

    /**
     * <p>创建 Agent Run。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return Agent Run 状态
     * @since 1.0.0
     */
    public RunStatus createRun(RunCreateRequest request) { return httpClient.createRun(request); }
    /**
     * <p>查询 Agent Run。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param runId Run 唯一标识
     * @return Agent Run 状态
     * @since 1.0.0
     */
    public RunStatus getRun(String runId) { return httpClient.getRun(runId); }
    /**
     * <p>停止 Agent Run。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param runId Run 唯一标识
     * @since 1.0.0
     */
    public void stopRun(String runId) { httpClient.stopRun(runId); }
    /**
     * <p>提交 Agent Run 审批决定。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param runId Run 唯一标识
     * @param decision 审批决定数据
     * @return 服务端确认审批后的 Run 数据
     * @since 1.0.0
     */
    public Map<String, Object> approveRun(String runId, Map<String, Object> decision) {
       
        return httpClient.approveRun(runId, decision);
    }

    // ============================================================
    // Streaming SSE (chat completions)
    // ============================================================

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request) {
        return chatCompletionStream(request, (Map<String, String>) null);
    }

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * <p>在基础鉴权请求头之外附加非空业务请求头；同步入口会等待底层请求完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                      Map<String, String> headers) {
        return chatCompletionStream(request, headers, null);
    }

    /**
     * <p>创建聊天补全 SSE 流式响应。</p>
     *
     * <p>在基础鉴权请求头之外附加非空业务请求头；同步入口会等待底层请求完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @param deltaConsumer 文本增量消费者
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStream(ChatRequest request,
                                                      Map<String, String> headers,
                                                      Consumer<String> deltaConsumer) {
        Objects.requireNonNull(request, "request");
        StreamingChatResponse stream = new StreamingChatResponse();
        stream.onDelta(deltaConsumer);
        SseSubscription subscription = sseClient.subscribeChat(
                request.withStream(), headers, stream::accept, stream::finish, stream::fail);
        stream.onCancel(subscription::close);
        stream.whenComplete((value, error) -> subscription.close());
        return stream;
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey, String sessionId) {
        return chatCompletionStreamWithSession(request, sessionKey, sessionId, null);
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionStreamWithSession(request, sessionKey, null, null);
    }

    /**
     * <p>在指定会话上下文中创建聊天补全 SSE 流式响应。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @param deltaConsumer 文本增量消费者
     * @return 可消费增量并等待完整文本的流式响应
     * @since 1.0.0
     */
    public StreamingChatResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId,
                                                                  Consumer<String> deltaConsumer) {
        return chatCompletionStream(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null), deltaConsumer);
    }

    // ============================================================
    // Session
    // ============================================================

    /**
     * <p>创建会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param title 会话标题；可以为 {@code null}
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public Session createSession(String title) { return httpClient.createSession(title); }
    /**
     * <p>列出会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public List<Session> listSessions() { return httpClient.listSessions(); }
    /**
     * <p>列出会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param limit 最大返回数量；可以为 {@code null}
     * @param offset 分页偏移量；可以为 {@code null}
     * @param source 会话来源过滤条件；可以为 {@code null}
     * @param includeChildren 是否包含子会话；可以为 {@code null}
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public List<Session> listSessions(Integer limit, Integer offset, String source, Boolean includeChildren) {
       
        return httpClient.listSessions(limit, offset, source, includeChildren);
    }
    /**
     * <p>查询会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param sessionId 会话唯一标识
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public Session getSession(String sessionId) { return httpClient.getSession(sessionId); }
    /**
     * <p>查询会话消息。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @return 按服务端顺序返回的会话消息列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> getSessionMessages(String id) { return httpClient.getSessionMessages(id); }
    /**
     * <p>派生会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @param title 会话标题；可以为 {@code null}
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public Session forkSession(String id, String title) { return httpClient.forkSession(id, title); }
    /**
     * <p>删除会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param sessionId 会话唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteSession(String sessionId) { return httpClient.deleteSession(sessionId); }
    /**
     * <p>在指定会话中发送聊天输入。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param sessionId 会话唯一标识
     * @param input 用户输入文本
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse sessionChat(String sessionId, String input) {
       
        return httpClient.sessionChat(sessionId, input);
    }

    // ============================================================
    // Jobs
    // ============================================================

    /**
     * <p>列出任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 后台任务定义与状态列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> listJobs() { return httpClient.listJobs(); }
    /**
     * <p>创建任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param job 任务定义
     * @return 服务端创建的任务数据
     * @since 1.0.0
     */
    public Map<String, Object> createJob(Map<String, Object> job) { return httpClient.createJob(job); }
    /**
     * <p>查询任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 指定任务的定义与当前状态
     * @since 1.0.0
     */
    public Map<String, Object> getJob(String jobId) { return httpClient.getJob(jobId); }
    /**
     * <p>更新任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @param patch 待更新字段集合
     * @return 应用补丁后的任务数据
     * @since 1.0.0
     */
    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) { return httpClient.updateJob(jobId, patch); }
    /**
     * <p>删除任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteJob(String jobId) { return httpClient.deleteJob(jobId); }
    /**
     * <p>暂停任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 服务端确认暂停后的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> pauseJob(String jobId) { return httpClient.pauseJob(jobId); }
    /**
     * <p>恢复任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 服务端确认恢复后的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> resumeJob(String jobId) { return httpClient.resumeJob(jobId); }
    /**
     * <p>立即执行任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 本次立即执行的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> runJobNow(String jobId) { return httpClient.runJobNow(jobId); }

    // ============================================================
    // SSE (raw access)
    // ============================================================

    /**
     * <p>返回聊天场景客户端。</p>
     *
     * @return 聊天场景客户端；HTTP 通道禁用时为 {@code null}
     * @since 1.0.0
     */
    public HermesChatClient chat() { return chatClient; }

    /**
     * <p>返回 SSE 客户端。</p>
     *
     * @return SSE 客户端；HTTP 通道禁用时为 {@code null}
     * @since 1.0.0
     */
    public HermesSseClient sse() { return sseClient; }

    // ============================================================
    // CLI
    // ============================================================

    /**
     * <p>返回本地 CLI 客户端。</p>
     *
     * @return 本地 CLI 客户端；CLI 通道禁用时为 {@code null}
     * @since 1.0.0
     */
    public HermesCli cli() {
        return cli;
    }

    // ============================================================
    // Config & lifecycle
    // ============================================================

    /**
     * <p>返回当前客户端配置快照。</p>
     *
     * @return 当前客户端配置快照
     * @since 1.0.0
     */
    public HermesClientConfig getConfig() { return config; }

    /**
     * <p>返回指定 Hermes Gateway profile 的托管客户端视图。</p>
     *
     * <p>profile 视图复用根客户端的 OkHttpClient 与 ObjectMapper，并由根客户端统一关闭。</p>
     *
     * @param profileId Gateway profile 标识
     * @return 复用根传输并由根客户端管理的 profile 客户端
     * @since 1.0.0
     */
    public HermesClient forProfile(String profileId) {
        if (managedProfileView) {
            throw new IllegalStateException("Cannot create a profile client from another profile client");
        }
        if (closed.get()) {
            throw new IllegalStateException("HermesClient is closed");
        }
        if (!isHttpEnabled()) {
            throw new IllegalStateException("Hermes HTTP client is disabled");
        }
        String normalizedProfileId = normalizeProfileId(profileId);
        // 并发访问同一 profile 时只发布一个托管视图。
        return profileClients.computeIfAbsent(normalizedProfileId, this::createProfileClient);
    }

    private HermesClient createProfileClient(String profileId) {
        HermesHttpClientConfig profileConfig = new HermesHttpClientConfig();
        copyHttpConfig(config.getHttp(), profileConfig);
        // profile 只改变 URL 前缀并禁用重复探测，传输和 JSON 配置继续复用根客户端。
        profileConfig.setBaseUrl(profileServerUrl(config.getHttp().getBaseUrl(), profileId));
        profileConfig.setStartupCheckEnabled(false);
        HermesCliConfig disabledCli = new HermesCliConfig();
        disabledCli.setEnabled(false);
        return new HermesClient(profileConfig, disabledCli, objectMapper, sharedHttpClient, false, true);
    }

    static String profileServerUrl(String serverUrl, String profileId) {
        if (Objects.isNull(serverUrl) || serverUrl.trim().isEmpty()) {
            throw new IllegalStateException("Hermes serverUrl must not be blank");
        }
        String normalizedServerUrl = serverUrl.trim().replaceAll("/+$", "");
        return normalizedServerUrl + "/p/" + normalizeProfileId(profileId);
    }

    private static String normalizeProfileId(String profileId) {
        if (Objects.isNull(profileId) || !PROFILE_ID_PATTERN.matcher(profileId.trim()).matches()) {
            throw new IllegalArgumentException("Invalid Hermes profileId: " + profileId);
        }
        return profileId.trim();
    }

    /**
     * <p>返回底层 OkHttpClient。</p>
     *
     * @return 由 SDK 管理生命周期的高并发 OkHttpClient
     * @since 1.0.0
     */
    public OkHttpClient getOkHttpClient() {
        return httpClient != null ? httpClient.getOkHttpClient() : null;
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
        if (managedProfileView || !closed.compareAndSet(false, true)) {
            return;
        }
        // 先停止所有托管视图，再释放根客户端拥有的共享连接资源。
        profileClients.values().forEach(HermesClient::closeManagedProfile);
        profileClients.clear();
        closeResources();
    }

    private void closeManagedProfile() {
        if (closed.compareAndSet(false, true)) {
            closeResources();
        }
    }

    private void closeResources() {
        if (sseClient != null) sseClient.close();
        if (httpClient != null) httpClient.close();
        HermesOkHttpClientFactory.shutdown(ownedHttpClient);
    }
}
