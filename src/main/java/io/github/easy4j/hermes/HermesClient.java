package io.github.easy4j.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.api.model.*;
import io.github.easy4j.hermes.cli.HermesCli;
import io.github.easy4j.hermes.cli.HermesCliExecutor;
import io.github.easy4j.hermes.api.HermesHttpClient;
import io.github.easy4j.hermes.api.HermesChatClient;
import io.github.easy4j.hermes.api.HermesSseClient;
import io.github.easy4j.hermes.api.model.ChatStreamingResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Hermes 客户端门面：HTTP REST + SSE 事件流 + 本地 CLI。
 */
@Slf4j
public class HermesClient implements AutoCloseable {

    private static final Pattern PROFILE_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private final HermesClientConfig config;
    private final HermesHttpClient httpClient;
    private final HermesChatClient chatClient;
    private final HermesSseClient sseClient;
    private final HermesCli cli;
    private final OkHttpClient ownedHttpClient;
    private final ObjectMapper objectMapper;
    private final OkHttpClient sharedHttpClient;
    private final boolean managedProfileView;
    private final ConcurrentMap<String, HermesClient> profileClients = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 使用组合配置构造客户端（推荐方式）。
     * <p>使用默认的 ObjectMapper 和 OkHttpClient。</p>
     *
     * @param config 组合配置，不得为 null
     */
    public HermesClient(HermesClientConfig config) {
        this(Objects.requireNonNull(config, "config").getHttp(), config.getCli(), new ObjectMapper(),
                HermesOkHttpClientFactory.create(config.getHttp()), true);
    }

    /**
     * 使用组合配置和调用方管理的共享 {@link OkHttpClient}。
     * <p>适用于直接注入 Spring 容器中由 okhttp3-extension/starter 配置的客户端。</p>
     *
     * @param config 组合配置
     * @param httpClient 外部共享 OkHttpClient
     */
    public HermesClient(HermesClientConfig config, OkHttpClient httpClient) {
        this(config, new ObjectMapper(), httpClient);
    }

    /**
     * 使用组合配置构造客户端。
     *
     * @param config        组合配置，不得为 null
     * @param objectMapper  共享 ObjectMapper，不得为 null
     * @param httpClient   共享 OkHttpClient，不得为 null
     */
    public HermesClient(HermesClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(Objects.requireNonNull(config, "config").getHttp(),
                config.getCli(),
                objectMapper,
                httpClient,
                false);
    }

    /**
     * 使用 HTTP 配置构造客户端（仅 HTTP，禁用 CLI）。
     * <p>使用默认的 ObjectMapper 和 OkHttpClient。</p>
     *
     * @param httpConfig HTTP 配置，不得为 null
     */
    public HermesClient(HermesHttpClientConfig httpConfig) {
        this(httpConfig, new HermesCliConfig());
    }

    /**
     * 使用 HTTP 配置构造客户端（仅 HTTP，禁用 CLI）。
     *
     * @param httpConfig   HTTP 配置，不得为 null
     * @param objectMapper 共享 ObjectMapper，不得为 null
     * @param httpClient   共享 OkHttpClient，不得为 null
     */
    public HermesClient(HermesHttpClientConfig httpConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(httpConfig, new HermesCliConfig(), objectMapper, httpClient);
    }

    /**
     * 使用 CLI 配置构造客户端（仅 CLI，禁用 HTTP）。
     * <p>使用默认的 ObjectMapper 和 OkHttpClient。</p>
     *
     * @param cliConfig CLI 配置，不得为 null
     */
    public HermesClient(HermesCliConfig cliConfig) {
        this(new HermesHttpClientConfig(), cliConfig);
    }

    /**
     * 使用 CLI 配置构造客户端（仅 CLI，禁用 HTTP）。
     *
     * @param cliConfig    CLI 配置，不得为 null
     * @param objectMapper 共享 ObjectMapper，不得为 null
     * @param httpClient   共享 OkHttpClient，不得为 null
     */
    public HermesClient(HermesCliConfig cliConfig, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(new HermesHttpClientConfig(), cliConfig, objectMapper, httpClient);
    }

    /**
     * 使用 HTTP 与 CLI 独立配置构造客户端。
     * <p>使用默认的 ObjectMapper 和 OkHttpClient。</p>
     *
     * @param httpConfig HTTP 配置，不得为 null
     * @param cliConfig  CLI 配置，不得为 null
     */
    public HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig) {
        this(httpConfig, cliConfig, new ObjectMapper(), HermesOkHttpClientFactory.create(httpConfig), true);
    }

    /**
     * 使用 HTTP/CLI 配置和调用方管理的共享 {@link OkHttpClient}。
     *
     * @param httpConfig HTTP 配置
     * @param cliConfig CLI 配置
     * @param httpClient 外部共享 OkHttpClient
     */
    public HermesClient(HermesHttpClientConfig httpConfig, HermesCliConfig cliConfig,
                        OkHttpClient httpClient) {
        this(httpConfig, cliConfig, new ObjectMapper(), httpClient);
    }

    /**
     * 使用 HTTP 与 CLI 独立配置构造客户端。
     *
     * @param httpConfig   HTTP 配置，不得为 null
     * @param cliConfig    CLI 配置，不得为 null
     * @param objectMapper 共享 ObjectMapper，不得为 null
     * @param httpClient   共享 OkHttpClient，不得为 null
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
        this.ownedHttpClient = ownsHttpClient ? httpClient : null;
        this.objectMapper = objectMapper;
        this.sharedHttpClient = httpClient;
        this.managedProfileView = managedProfileView;
        this.config = new HermesClientConfig();
        copyHttpConfig(httpConfig);
        copyCliConfig(cliConfig);

        // HTTP 客户端初始化
        if (httpConfig.isEnabled()) {
            this.chatClient = new HermesChatClient(httpConfig, objectMapper, httpClient);
            this.httpClient = this.chatClient;
            this.sseClient = this.chatClient.events();
            // HTTP 启动检查
            if (httpConfig.isStartupCheckEnabled()) {
                try {
                    HealthStatus status = this.httpClient.health();
                    if (!"ok".equals(status.getStatus())) {
                        handleHttpCheckFailed(httpConfig, "Health check failed: " + status.getStatus());
                    } else {
                        log.info("Hermes HTTP health check passed: serverUrl={}, defaultModel={}",
                                httpConfig.getServerUrl(), httpConfig.getDefaultModel());
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

        // CLI 初始化
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
     * 全量注入构造，供测试或高级定制使用。
     * <p>使用此构造方法不会执行任何启动检查。</p>
     *
     * @param config    组合配置，不得为 null
     * @param httpClient HTTP 客户端实例，不得为 null
     * @param sseClient SSE 客户端实例，不得为 null
     * @param cli       CLI 实例，不得为 null
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
        target.setServerUrl(src.getServerUrl());
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
        target.setRetryOnConnectionFailure(src.isRetryOnConnectionFailure());
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

    public boolean isHttpEnabled() { return httpClient != null; }
    public boolean isCliEnabled() { return cli != null; }

    // ============================================================
    // Health
    // ============================================================

    public HealthStatus health() { return httpClient.health(); }
    public HealthStatus healthDetailed() { return httpClient.healthDetailed(); }
    public HealthStatus healthV1() { return httpClient.healthV1(); }

    // ============================================================
    // Chat Completions
    // ============================================================

    public ChatResponse chatCompletion(ChatRequest request) {
       
        return httpClient.chatCompletion(request);
    }

    /** Chat completion with Hermes custom headers. */
    public ChatResponse chatCompletion(ChatRequest request,
                                       Map<String, String> headers) {
       
        return httpClient.chatCompletion(request, headers);
    }

    /** Convenience: chat with session key. */
    public ChatResponse chatCompletionWithSession(ChatRequest request,
                                                  String sessionKey,
                                                  String sessionId) {
       
        return httpClient.chatCompletion(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null));
    }

    /**
     * 按 sessionKey 发送消息并同步等待 AI 响应（2 参数版）。
     * <p>与 OpenClaw/OpenCode 的 {@code chatCompletionWithSession(request, sessionKey)} 对称。</p>
     *
     * @param request    请求体
     * @param sessionKey 会话路由 key
     * @return Chat 响应
     */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionWithSession(request, sessionKey, (String) null);
    }

    /** 按 sessionKey 调用，并将业务取消信号传播到底层 HTTP Call。 */
    public ChatResponse chatCompletionWithSession(ChatRequest request, String sessionKey,
                                                  HttpCallCancellation cancellation) {
        return httpClient.chatCompletion(request,
                HermesHttpClient.hermesHeaders(sessionKey, null, null), cancellation);
    }

    // ============================================================
    // Responses API
    // ============================================================

    public ResponseResult createResponse(ResponseRequest request) {
       
        return httpClient.createResponse(request);
    }

    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
       
        return httpClient.createResponse(request, headers);
    }

    public ResponseResult getResponse(String responseId) { return httpClient.getResponse(responseId); }
    public boolean deleteResponse(String responseId) { return httpClient.deleteResponse(responseId); }

    // ============================================================
    // Models & Capabilities & Skills
    // ============================================================

    public ModelsResponse listModels() { return httpClient.listModels(); }
    public CapabilityInfo getCapabilities() { return httpClient.getCapabilities(); }
    public List<Map<String, Object>> listSkills() { return httpClient.listSkills(); }
    public List<Map<String, Object>> listToolsets() { return httpClient.listToolsets(); }

    // ============================================================
    // Run
    // ============================================================

    public RunStatus createRun(RunCreateRequest request) { return httpClient.createRun(request); }
    public RunStatus getRun(String runId) { return httpClient.getRun(runId); }
    public void stopRun(String runId) { httpClient.stopRun(runId); }
    public Map<String, Object> approveRun(String runId, Map<String, Object> decision) {
       
        return httpClient.approveRun(runId, decision);
    }

    // ============================================================
    // Streaming SSE (chat completions)
    // ============================================================

    /**
     * Streaming chat completion returning a {@link ChatStreamingResponse} that accumulates
     * delta text and completes when the stream ends.
     */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request) {
        return chatCompletionStream(request, (Map<String, String>) null);
    }

    /** Streaming chat completion with Hermes custom headers. */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request,
                                                      Map<String, String> headers) {
        return chatCompletionStream(request, headers, null);
    }

    /** Streaming chat completion，订阅启动前绑定增量回调，避免丢失首批分片。 */
    public ChatStreamingResponse chatCompletionStream(ChatRequest request,
                                                      Map<String, String> headers,
                                                      Consumer<String> deltaConsumer) {
        Objects.requireNonNull(request, "request");
        ChatStreamingResponse stream = new ChatStreamingResponse(
                config.getHttp().getStreamEventQueueCapacity());
        stream.onDelta(deltaConsumer);
        sseClient.subscribeChat(request.withStream(), headers, stream::accept, stream::finish, stream::fail);
        return stream;
    }

    /** Convenience: streaming chat with session key. */
    public ChatStreamingResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey, String sessionId) {
        return chatCompletionStreamWithSession(request, sessionKey, sessionId, null);
    }

    /**
     * 按 sessionKey 流式 chat completion（2 参数版，对齐 OpenClaw/OpenCode）。
     */
    public ChatStreamingResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionStreamWithSession(request, sessionKey, null, null);
    }

    /** 按 sessionKey 流式对话，并在订阅启动前绑定增量回调。 */
    public ChatStreamingResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey,
                                                                  String sessionId,
                                                                  Consumer<String> deltaConsumer) {
        return chatCompletionStream(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null), deltaConsumer);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) { return httpClient.createSession(title); }
    public List<Session> listSessions() { return httpClient.listSessions(); }
    /** 分页列出 sessions。 */
    public List<Session> listSessions(Integer limit, Integer offset, String source, Boolean includeChildren) {
       
        return httpClient.listSessions(limit, offset, source, includeChildren);
    }
    public Session getSession(String sessionId) { return httpClient.getSession(sessionId); }
    public List<Map<String, Object>> getSessionMessages(String id) { return httpClient.getSessionMessages(id); }
    public Session forkSession(String id, String title) { return httpClient.forkSession(id, title); }
    public boolean deleteSession(String sessionId) { return httpClient.deleteSession(sessionId); }
    public ChatResponse sessionChat(String sessionId, String input) {
       
        return httpClient.sessionChat(sessionId, input);
    }

    // ============================================================
    // Jobs
    // ============================================================

    public List<Map<String, Object>> listJobs() { return httpClient.listJobs(); }
    public Map<String, Object> createJob(Map<String, Object> job) { return httpClient.createJob(job); }
    public Map<String, Object> getJob(String jobId) { return httpClient.getJob(jobId); }
    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) { return httpClient.updateJob(jobId, patch); }
    public boolean deleteJob(String jobId) { return httpClient.deleteJob(jobId); }
    public Map<String, Object> pauseJob(String jobId) { return httpClient.pauseJob(jobId); }
    public Map<String, Object> resumeJob(String jobId) { return httpClient.resumeJob(jobId); }
    public Map<String, Object> runJobNow(String jobId) { return httpClient.runJobNow(jobId); }

    // ============================================================
    // SSE (raw access)
    // ============================================================

    /** 获取统一的 Hermes 聊天场景客户端。 */
    public HermesChatClient chat() { return chatClient; }

    /** @deprecated 业务聊天请使用 {@link #chat()}，这里只保留原始事件订阅兼容入口。 */
    @Deprecated
    public HermesSseClient sse() { return sseClient; }

    // ============================================================
    // CLI
    // ============================================================

    public HermesCli cli() {
        return cli;
    }

    // ============================================================
    // Config & lifecycle
    // ============================================================

    public HermesClientConfig getConfig() { return config; }

    /**
     * 返回由当前根客户端托管的 Hermes multiplex profile 客户端。
     * <p>profile 客户端复用根客户端的 OkHttp 连接池和 ObjectMapper，请只关闭根客户端。</p>
     *
     * @param profileId Hermes profile 标识，例如 {@code sales}
     * @return 基础路径为 {@code /p/<profileId>} 的客户端
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
        return profileClients.computeIfAbsent(normalizedProfileId, this::createProfileClient);
    }

    private HermesClient createProfileClient(String profileId) {
        HermesHttpClientConfig profileConfig = new HermesHttpClientConfig();
        copyHttpConfig(config.getHttp(), profileConfig);
        profileConfig.setServerUrl(profileServerUrl(config.getHttp().getServerUrl(), profileId));
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
     * 返回 HTTP 与 SSE 共用的 OkHttpClient；HTTP 未启用时返回 null。
     *
     * @return 实际使用的 OkHttpClient
     */
    public OkHttpClient getOkHttpClient() {
        return httpClient != null ? httpClient.getOkHttpClient() : null;
    }

    @Override
    public void close() {
        if (managedProfileView || !closed.compareAndSet(false, true)) {
            return;
        }
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
        if (httpClient != null) httpClient.close();
        if (sseClient != null) sseClient.close();
        HermesOkHttpClientFactory.shutdown(ownedHttpClient);
    }
}
