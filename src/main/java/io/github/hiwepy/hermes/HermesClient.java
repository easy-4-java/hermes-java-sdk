package io.github.hiwepy.hermes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.hermes.api.model.*;
import io.github.hiwepy.hermes.cli.HermesCli;
import io.github.hiwepy.hermes.cli.HermesCliExecutor;
import io.github.hiwepy.hermes.api.HermesHttpClient;
import io.github.hiwepy.hermes.api.HermesSseClient;
import io.github.hiwepy.hermes.api.model.ChatStreamingResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Hermes 客户端门面：HTTP REST + SSE 事件流 + 本地 CLI。
 */
@Slf4j
public class HermesClient implements AutoCloseable {

    private final HermesClientConfig config;
    private final HermesHttpClient httpClient;
    private final HermesSseClient sseClient;
    private final HermesCli cli;

    /**
     * 使用配置构造客户端（推荐方式）。
     * <p>使用默认的 ObjectMapper 和 OkHttpClient。</p>
     *
     * @param config 配置，不得为 null
     */
    public HermesClient(HermesClientConfig config) {
        this(config, new ObjectMapper(), new OkHttpClient());
    }

    /**
     * 使用配置构造客户端。
     *
     * @param config        配置，不得为 null
     * @param objectMapper 共享 ObjectMapper，不得为 null
     * @param httpClient   共享 OkHttpClient，不得为 null
     */
    public HermesClient(HermesClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(httpClient, "httpClient");

        // HTTP 客户端初始化
        if (config.isHttpEnabled()) {
            this.httpClient = new HermesHttpClient(config, objectMapper, httpClient);
            this.sseClient = new HermesSseClient(config, objectMapper, httpClient);
            // HTTP 启动检查
            if (config.isHttpStartupCheckEnabled()) {
                try {
                    HealthStatus status = this.httpClient.health();
                    if (!"ok".equals(status.getStatus())) {
                        handleHttpCheckFailed(config, "Health check failed: " + status.getStatus());
                    }
                } catch (Exception e) {
                    handleHttpCheckFailed(config, "Health check failed: " + e.getMessage());
                }
            }
        } else {
            this.httpClient = null;
            this.sseClient = null;
        }

        // CLI 初始化
        if (config.isCliEnabled()) {
            HermesCliExecutor executor = new HermesCliExecutor(config);
            boolean cliAvailable = !config.isCliStartupCheckEnabled() || executor.probe();
            if (!cliAvailable) {
                handleCliCheckFailed(config);
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
     * @param config    配置，不得为 null
     * @param httpClient HTTP 客户端实例，不得为 null
     * @param sseClient SSE 客户端实例，不得为 null
     * @param cli       CLI 实例，不得为 null
     */
    public HermesClient(HermesClientConfig config, HermesHttpClient httpClient,
                        HermesSseClient sseClient, HermesCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.sseClient = Objects.requireNonNull(sseClient, "sseClient");
        this.cli = Objects.requireNonNull(cli, "cli");
    }

    private void handleHttpCheckFailed(HermesClientConfig config, String message) {
        if (config.isHttpFailFastOnUnavailable()) {
            throw new IllegalStateException("Hermes HTTP service is not available: " + message
                    + ". Set httpEnabled=false or httpStartupCheckEnabled=false to disable.");
        }
        log.warn("Hermes HTTP service is not available: {} (continuing without HTTP support)", message);
    }

    private void handleCliCheckFailed(HermesClientConfig config) {
        if (config.isCliFailFastOnUnavailable()) {
            throw new IllegalStateException("Hermes CLI is not available: " + config.getLocalExecutable()
                    + ". Set cliEnabled=false or cliStartupCheckEnabled=false to disable.");
        }
        log.warn("Hermes CLI is not available: {} (continuing without CLI support)", config.getLocalExecutable());
    }

    // ============================================================
    // Status checks
    // ============================================================

    public boolean isHttpEnabled() { return httpClient != null; }
    public boolean isCliEnabled() { return cli != null; }

    private void checkHttpEnabled() {
        if (httpClient == null) {
            throw new IllegalStateException("HTTP client is disabled. Set HermesClientConfig.httpEnabled=true to enable.");
        }
    }

    private void checkCliEnabled() {
        if (cli == null) {
            throw new IllegalStateException("CLI client is disabled. Set HermesClientConfig.cliEnabled=true to enable.");
        }
    }

    // ============================================================
    // Health
    // ============================================================

    public HealthStatus health() { checkHttpEnabled(); return httpClient.health(); }
    public HealthStatus healthDetailed() { checkHttpEnabled(); return httpClient.healthDetailed(); }
    public HealthStatus healthV1() { checkHttpEnabled(); return httpClient.healthV1(); }

    // ============================================================
    // Chat Completions
    // ============================================================

    public ChatResponse chatCompletion(ChatRequest request) {
        checkHttpEnabled();
        return httpClient.chatCompletion(request);
    }

    /** Chat completion with Hermes custom headers. */
    public ChatResponse chatCompletion(ChatRequest request,
                                       Map<String, String> headers) {
        checkHttpEnabled();
        return httpClient.chatCompletion(request, headers);
    }

    /** Convenience: chat with session key. */
    public ChatResponse chatCompletionWithSession(ChatRequest request,
                                                  String sessionKey,
                                                  String sessionId) {
        checkHttpEnabled();
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
        return chatCompletionWithSession(request, sessionKey, null);
    }

    // ============================================================
    // Responses API
    // ============================================================

    public ResponseResult createResponse(ResponseRequest request) {
        checkHttpEnabled();
        return httpClient.createResponse(request);
    }

    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
        checkHttpEnabled();
        return httpClient.createResponse(request, headers);
    }

    public ResponseResult getResponse(String responseId) { checkHttpEnabled(); return httpClient.getResponse(responseId); }
    public boolean deleteResponse(String responseId) { checkHttpEnabled(); return httpClient.deleteResponse(responseId); }

    // ============================================================
    // Models & Capabilities & Skills
    // ============================================================

    public ModelsResponse listModels() { checkHttpEnabled(); return httpClient.listModels(); }
    public CapabilityInfo getCapabilities() { checkHttpEnabled(); return httpClient.getCapabilities(); }
    public List<Map<String, Object>> listSkills() { checkHttpEnabled(); return httpClient.listSkills(); }
    public List<Map<String, Object>> listToolsets() { checkHttpEnabled(); return httpClient.listToolsets(); }

    // ============================================================
    // Run
    // ============================================================

    public RunStatus createRun(RunCreateRequest request) { checkHttpEnabled(); return httpClient.createRun(request); }
    public RunStatus getRun(String runId) { checkHttpEnabled(); return httpClient.getRun(runId); }
    public void stopRun(String runId) { checkHttpEnabled(); httpClient.stopRun(runId); }
    public Map<String, Object> approveRun(String runId, Map<String, Object> decision) {
        checkHttpEnabled();
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
        checkHttpEnabled();
        request.setStream(true);
        ChatStreamingResponse stream = new ChatStreamingResponse();
        sseClient.subscribeChat(request, headers, stream::accept, stream::finish, stream::fail);
        return stream;
    }

    /** Convenience: streaming chat with session key. */
    public ChatStreamingResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey, String sessionId) {
        return chatCompletionStream(request,
                HermesHttpClient.hermesHeaders(sessionKey, sessionId, null));
    }

    /**
     * 按 sessionKey 流式 chat completion（2 参数版，对齐 OpenClaw/OpenCode）。
     */
    public ChatStreamingResponse chatCompletionStreamWithSession(ChatRequest request, String sessionKey) {
        return chatCompletionStreamWithSession(request, sessionKey, null);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) { checkHttpEnabled(); return httpClient.createSession(title); }
    public List<Session> listSessions() { checkHttpEnabled(); return httpClient.listSessions(); }
    /** 分页列出 sessions。 */
    public List<Session> listSessions(Integer limit, Integer offset, String source, Boolean includeChildren) {
        checkHttpEnabled();
        return httpClient.listSessions(limit, offset, source, includeChildren);
    }
    public Session getSession(String sessionId) { checkHttpEnabled(); return httpClient.getSession(sessionId); }
    public List<Map<String, Object>> getSessionMessages(String id) { checkHttpEnabled(); return httpClient.getSessionMessages(id); }
    public Session forkSession(String id, String title) { checkHttpEnabled(); return httpClient.forkSession(id, title); }
    public boolean deleteSession(String sessionId) { checkHttpEnabled(); return httpClient.deleteSession(sessionId); }
    public ChatResponse sessionChat(String sessionId, String input) {
        checkHttpEnabled();
        return httpClient.sessionChat(sessionId, input);
    }

    // ============================================================
    // Jobs
    // ============================================================

    public List<Map<String, Object>> listJobs() { checkHttpEnabled(); return httpClient.listJobs(); }
    public Map<String, Object> createJob(Map<String, Object> job) { checkHttpEnabled(); return httpClient.createJob(job); }
    public Map<String, Object> getJob(String jobId) { checkHttpEnabled(); return httpClient.getJob(jobId); }
    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) { checkHttpEnabled(); return httpClient.updateJob(jobId, patch); }
    public boolean deleteJob(String jobId) { checkHttpEnabled(); return httpClient.deleteJob(jobId); }
    public Map<String, Object> pauseJob(String jobId) { checkHttpEnabled(); return httpClient.pauseJob(jobId); }
    public Map<String, Object> resumeJob(String jobId) { checkHttpEnabled(); return httpClient.resumeJob(jobId); }
    public Map<String, Object> runJobNow(String jobId) { checkHttpEnabled(); return httpClient.runJobNow(jobId); }

    // ============================================================
    // SSE (raw access)
    // ============================================================

    public HermesSseClient sse() { checkHttpEnabled(); return sseClient; }

    // ============================================================
    // CLI
    // ============================================================

    public HermesCli cli() {
        checkCliEnabled();
        return cli;
    }

    // ============================================================
    // Config & lifecycle
    // ============================================================

    public HermesClientConfig getConfig() { return config; }

    @Override
    public void close() {
        if (httpClient != null) httpClient.close();
        if (sseClient != null) sseClient.close();
    }
}
    public void close() { httpClient.close(); sseClient.close(); }
}
