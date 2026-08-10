package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesHttpClientConfig;
import io.github.easy4j.hermes.HttpCallCancellation;
import io.github.easy4j.hermes.HermesOkHttpClientFactory;
import static io.github.easy4j.hermes.api.HermesApiConstants.*;
import io.github.easy4j.hermes.api.model.*;
import io.github.easy4j.hermes.exception.HermesHttpException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import okio.Buffer;

/**
 * <p>Hermes Server REST API 客户端。</p>
 *
 * <p>基于 OkHttp 提供同步与异步请求、取消传播、安全追踪日志、JSON 序列化以及调用方注入传输的复用能力。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesHttpClient implements AutoCloseable {

    /**
     * 请求体 JSON 媒体类型，使用 UTF-8 编码。
     */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /**
     * 为并发 HTTP 调用生成日志关联编号的无锁原子序列。
     */
    private static final AtomicLong REQUEST_SEQUENCE = new AtomicLong();

    /**
     * 当前客户端使用的配置快照。
     */
    private final HermesHttpClientConfig config;
    /**
     * JSON 序列化与反序列化使用的 ObjectMapper。
     */
    @Getter
    private final ObjectMapper objectMapper;
    /**
     * 执行 HTTP 请求的 OkHttpClient。
     */
    private final OkHttpClient httpClient;
    /**
     * 当前对象是否负责关闭 HTTP 客户端。
     */
    private final boolean ownsHttpClient;

    /**
     * <p>创建 HermesHttpClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesHttpClient(HermesHttpClientConfig config) {
        this(config, null, HermesOkHttpClientFactory.create(config), true);
    }

    /**
     * <p>创建 HermesHttpClient 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @param objectMapper 用于 JSON 序列化和反序列化的共享 ObjectMapper
     * @param httpClient 调用方提供或 SDK 创建的 OkHttpClient
     * @since 1.0.0
     */
    public HermesHttpClient(HermesHttpClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this(config, objectMapper,
                Objects.isNull(httpClient) ? HermesOkHttpClientFactory.create(config) : httpClient,
                Objects.isNull(httpClient));
    }

    private HermesHttpClient(HermesHttpClientConfig config, ObjectMapper objectMapper,
                             OkHttpClient httpClient, boolean ownsHttpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.objectMapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.ownsHttpClient = ownsHttpClient;
        log.debug("Hermes HTTP client initialized: baseUrl={}, connectTimeoutMs={}, readTimeoutMs={}, "
                        + "callTimeoutMs={}, retryOnConnectionFailure={}, detailedLoggingEnabled={}",
                config.getBaseUrl(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis(),
                config.getCallTimeoutMillis(), config.isRetryOnConnectionFailure(),
                config.isDetailedLoggingEnabled());
    }

    // ============================================================
    // Global / Health
    // ============================================================

    /**
     * <p>查询 Hermes 基础健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus health() { return awaitFuture(healthAsync()); }
    /**
     * <p>异步查询 Hermes 基础健康状态。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @return 异步承载服务健康状态的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<HealthStatus> healthAsync() { return getAsync(PATH_HEALTH, HealthStatus.class); }
    /**
     * <p>查询 Hermes 详细健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus healthDetailed() { return get(PATH_HEALTH_DETAILED, HealthStatus.class); }
    /**
     * <p>查询 Hermes V1 健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务健康状态
     * @since 1.0.0
     */
    public HealthStatus healthV1() { return get(PATH_V1_HEALTH, HealthStatus.class); }

    // ============================================================
    // Chat Completion
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
        return post(PATH_CHAT_COMPLETIONS, request, ChatResponse.class);
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
    public ChatResponse chatCompletion(ChatRequest request, Map<String, String> headers) {
        return post(PATH_CHAT_COMPLETIONS, request, ChatResponse.class, headers);
    }

    /**
     * <p>执行聊天补全请求。</p>
     *
     * <p>附加非空业务请求头，将取消信号传播到底层 Call，并在当前线程等待异步请求完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @param cancellation 调用取消信号；可以为 {@code null}
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse chatCompletion(ChatRequest request, Map<String, String> headers,
                                       HttpCallCancellation cancellation) {
        return awaitFuture(chatCompletionAsync(request, headers, cancellation));
    }

    /**
     * <p>异步执行聊天补全请求。</p>
     *
     * <p>附加非空业务请求头，将业务取消信号传播到底层 Call，并通过 OkHttp enqueue 非阻塞提交；失败通过 CompletableFuture 异常完成。</p>
     *
     * @param request 请求对象，不得为 {@code null}
     * @param headers 附加请求头；可以为 {@code null}
     * @param cancellation 调用取消信号；可以为 {@code null}
     * @return 异步承载类型化聊天补全响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ChatResponse> chatCompletionAsync(ChatRequest request,
                                                               Map<String, String> headers,
                                                               HttpCallCancellation cancellation) {
        return postAsync(PATH_CHAT_COMPLETIONS, request, ChatResponse.class, headers, cancellation);
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
        return chatCompletionAsync(request, null, null);
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
        return awaitFuture(createResponseAsync(request));
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
        return postAsync(PATH_RESPONSES, request, ResponseResult.class, null, null);
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
        return post(PATH_RESPONSES, request, ResponseResult.class, headers);
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
        return postAsync(PATH_RESPONSES, request, ResponseResult.class, headers, null);
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
    public ResponseResult getResponse(String responseId) {
        return get("/v1/responses/" + responseId, ResponseResult.class);
    }

    /**
     * <p>删除指定 Responses API 响应。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param responseId 响应唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteResponse(String responseId) {
        return deleteBoolean(PATH_RESPONSES + "/" + responseId);
    }

    // ============================================================
    // Models & Capabilities
    // ============================================================

    /**
     * <p>列出 Hermes 可用模型。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 模型列表响应
     * @since 1.0.0
     */
    public ModelsResponse listModels() { return awaitFuture(listModelsAsync()); }
    /**
     * <p>异步列出 Hermes 可用模型。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @return 异步承载模型列表响应的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<ModelsResponse> listModelsAsync() { return getAsync(PATH_MODELS, ModelsResponse.class); }

    private static String encodePathSegment(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * <p>查询指定模型。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param modelId 模型唯一标识
     * @return 模型列表响应
     * @since 1.0.0
     */
    public ModelsResponse.ModelData getModel(String modelId) {
        return get(PATH_MODELS + "/" + encodePathSegment(modelId),
                ModelsResponse.ModelData.class);
    }

    /**
     * <p>查询 Hermes 服务能力。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return Hermes 服务能力信息
     * @since 1.0.0
     */
    public CapabilityInfo getCapabilities() { return get(PATH_CAPABILITIES, CapabilityInfo.class); }

    // ============================================================
    // Skills & Toolsets
    // ============================================================

    /**
     * <p>列出可用技能。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 服务端已加载技能的描述列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> listSkills() {
        return getList(PATH_SKILLS, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * <p>列出可用工具集。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 服务端可用工具集的描述列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> listToolsets() {
        return getList(PATH_TOOLSETS, new TypeReference<List<Map<String, Object>>>() {});
    }

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
    public RunStatus createRun(RunCreateRequest request) {
        return post(PATH_RUNS, request, RunStatus.class);
    }

    /**
     * <p>查询 Agent Run。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param runId Run 唯一标识
     * @return Agent Run 状态
     * @since 1.0.0
     */
    public RunStatus getRun(String runId) { return get("/v1/runs/" + runId, RunStatus.class); }

    /**
     * <p>停止 Agent Run。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param runId Run 唯一标识
     * @since 1.0.0
     */
    public void stopRun(String runId) {
        awaitFuture(stopRunAsync(runId));
    }

    /**
     * <p>异步停止 Agent Run。</p>
     *
     * <p>请求通过 OkHttp enqueue 非阻塞提交；网络、状态码或反序列化失败通过 CompletableFuture 异常完成。</p>
     *
     * @param runId Run 唯一标识
     * @return Run 停止请求完成时以 {@code null} 成功结束的 CompletableFuture
     * @since 1.0.0
     */
    public CompletableFuture<Void> stopRunAsync(String runId) {
        Request request = authedRequest(url(PATH_RUNS + "/" + runId + "/stop"))
                .post(RequestBody.create(new byte[0], null)).build();
        return executeResponseAsync(request, null).thenApply(response -> {
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.getStatusCode(), response.getBody());
            }
            return null;
        });
    }

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
        return postMap(PATH_RUNS + "/" + runId + "/approval", decision);
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
    public Session createSession(String title) {
        Map<String, Object> body = title != null ? Collections.singletonMap("title", title) : Collections.emptyMap();
        return post(PATH_SESSIONS, body, Session.class);
    }

    /**
     * <p>列出会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public List<Session> listSessions() {
        return getList(PATH_SESSIONS, new TypeReference<List<Session>>() {});
    }

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
        HttpUrl.Builder urlBuilder = HttpUrl.get(url(PATH_SESSIONS)).newBuilder();
        if (limit != null) urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        if (offset != null) urlBuilder.addQueryParameter("offset", String.valueOf(offset));
        if (source != null) urlBuilder.addQueryParameter("source", source);
        if (includeChildren != null) urlBuilder.addQueryParameter("include_children", String.valueOf(includeChildren));
        Request request = authedRequest(urlBuilder.build().toString()).get().build();
        return executeList(request, new TypeReference<List<Session>>() {});
    }

    /**
     * <p>查询会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public Session getSession(String id) { return get(PATH_SESSIONS + "/" + id, Session.class); }

    /**
     * <p>查询会话消息。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @return 按服务端顺序返回的会话消息列表
     * @since 1.0.0
     */
    public List<Map<String, Object>> getSessionMessages(String id) {
        return getList(PATH_SESSIONS + "/" + id + "/messages",
                new TypeReference<List<Map<String, Object>>>() {});
    }

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
    public Session forkSession(String id, String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (title != null) body.put("title", title);
        return post(PATH_SESSIONS + "/" + id + "/fork", body, Session.class);
    }

    /**
     * <p>删除会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteSession(String id) {
        return deleteBoolean(PATH_SESSIONS + "/" + id);
    }

    /**
     * <p>更新会话。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @param patch 待更新字段集合
     * @return 会话对象或会话列表
     * @since 1.0.0
     */
    public Session updateSession(String id, Map<String, Object> patch) {
        Request request = authedRequest(url(PATH_SESSIONS + "/" + id))
                .patch(RequestBody.create(toJson(patch), JSON)).build();
        return execute(request, Session.class);
    }

    /**
     * <p>在指定会话中发送聊天输入。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param id 对象唯一标识
     * @param input 用户输入文本
     * @return 聊天补全响应
     * @since 1.0.0
     */
    public ChatResponse sessionChat(String id, String input) {
        return post(PATH_SESSIONS + "/" + id + "/chat", Collections.singletonMap("input", input), ChatResponse.class);
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
    public List<Map<String, Object>> listJobs() {
        return getList(PATH_JOBS, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * <p>创建任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param job 任务定义
     * @return 服务端创建的任务数据
     * @since 1.0.0
     */
    public Map<String, Object> createJob(Map<String, Object> job) {
        return postMap(PATH_JOBS, job);
    }

    /**
     * <p>查询任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 指定任务的定义与当前状态
     * @since 1.0.0
     */
    public Map<String, Object> getJob(String jobId) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId)).get().build();
        return executeList(request, new TypeReference<Map<String, Object>>() {});
    }

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
    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId))
                .patch(RequestBody.create(toJson(patch), JSON)).build();
        return executeList(request, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * <p>删除任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 删除成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean deleteJob(String jobId) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId)).delete().build();
        return awaitFuture(executeResponseAsync(request, null).thenApply(response -> {
            if (!response.isSuccessful() && response.getStatusCode() != 404) {
                log.warn("deleteJob {} failed: {}", jobId, response.getStatusCode());
            }
            return response.isSuccessful();
        }));
    }

    /**
     * <p>暂停任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 服务端确认暂停后的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> pauseJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/pause", Collections.emptyMap());
    }

    /**
     * <p>恢复任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 服务端确认恢复后的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> resumeJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/resume", Collections.emptyMap());
    }

    /**
     * <p>立即执行任务。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @param jobId 任务唯一标识
     * @return 本次立即执行的任务状态
     * @since 1.0.0
     */
    public Map<String, Object> runJobNow(String jobId) {
        return postMap("/api/jobs/" + jobId + "/run", Collections.emptyMap());
    }

    // ============================================================
    // Hermes-specific headers
    // ============================================================

    /**
     * <p>构造 Hermes 会话上下文请求头。</p>
     *
     * @param sessionKey 业务会话键
     * @param sessionId 会话唯一标识
     * @param messageChannel 消息通道标识
     * @return 仅包含非空会话上下文字段的请求头映射
     * @since 1.0.0
     */
    public static Map<String, String> hermesHeaders(String sessionKey, String sessionId, String messageChannel) {
        Map<String, String> h = new LinkedHashMap<>();
        if (sessionKey != null) h.put(HEADER_SESSION_KEY, sessionKey);
        if (sessionId != null) h.put(HEADER_SESSION_ID, sessionId);
        if (messageChannel != null) h.put(HEADER_MESSAGE_CHANNEL, messageChannel);
        return h;
    }

    /**
     * <p>返回底层 OkHttpClient。</p>
     *
     * @return 由 SDK 管理生命周期的高并发 OkHttpClient
     * @since 1.0.0
     */
    public OkHttpClient getOkHttpClient() {
        return httpClient;
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getBaseUrl() + path;
    }

    private Request.Builder authedRequest(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        String apiKey = config.resolveApiKey();
        if (!apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    private <T> T get(String path, Class<T> type) {
        return awaitFuture(getAsync(path, type));
    }

    private <T> CompletableFuture<T> getAsync(String path, Class<T> type) {
        Request request = authedRequest(url(path)).get().build();
        return executeAsync(request, type, null);
    }

    private <T> T getList(String path, TypeReference<T> typeRef) {
        Request request = authedRequest(url(path)).get().build();
        return executeList(request, typeRef);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        Request request = authedRequest(url(path))
                .post(RequestBody.create(toJson(body), JSON)).build();
        return execute(request, type);
    }

    private <T> T post(String path, Object body, Class<T> type, Map<String, String> headers) {
        return post(path, body, type, headers, null);
    }

    private <T> T post(String path, Object body, Class<T> type, Map<String, String> headers, HttpCallCancellation cancellation) {
        return awaitFuture(postAsync(path, body, type, headers, cancellation));
    }

    private <T> CompletableFuture<T> postAsync(String path, Object body, Class<T> type,
                                               Map<String, String> headers,
                                               HttpCallCancellation cancellation) {
        Request.Builder builder = authedRequest(url(path));
        if (headers != null) {
            headers.forEach((k, v) -> { if (k != null && v != null) builder.header(k, v); });
        }
        Request request = builder.post(RequestBody.create(toJson(body), JSON)).build();
        return executeAsync(request, type, cancellation);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postMap(String path, Object body) {
        Request request = authedRequest(url(path))
                .post(RequestBody.create(toJson(body), JSON)).build();
        return executeList(request, new TypeReference<Map<String, Object>>() {});
    }

    private boolean deleteBoolean(String path) {
        Request request = authedRequest(url(path)).delete().build();
        return awaitFuture(executeResponseAsync(request, null).thenApply(HttpResponseData::isSuccessful));
    }

    private <T> T execute(Request request, Class<T> type) {
        return execute(request, type, null);
    }

    private <T> T execute(Request request, Class<T> type, HttpCallCancellation cancellation) {
        return awaitFuture(executeAsync(request, type, cancellation));
    }

    /**
     * <p>异步执行 HTTP 请求并反序列化响应对象。</p>
     *
     * <p>业务取消信号会传播到底层 OkHttp Call，避免已无消费者的请求继续占用连接。</p>
     *
     * @param <T> 方法返回或异步承载的目标类型
     * @param request 请求对象，不得为 {@code null}
     * @param type 事件类型或目标 Java 类型
     * @param cancellation 调用取消信号；可以为 {@code null}
     * @return 异步承载目标 Java 类型响应的 CompletableFuture
     * @since 1.0.0
     */
    protected <T> CompletableFuture<T> executeAsync(Request request, Class<T> type,
                                                    HttpCallCancellation cancellation) {
        return executeResponseAsync(request, cancellation).thenApply(response -> {
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.getStatusCode(), response.getBody());
            }
            try {
                return objectMapper.readValue(response.getBody(), type);
            } catch (IOException error) {
                throw new HermesHttpException("Failed to parse response: " + error.getMessage(), error);
            }
        });
    }

    private CompletableFuture<HttpResponseData> executeResponseAsync(Request request,
                                                                     HttpCallCancellation cancellation) {
        long requestId = beginTrace(request);
        long startedAt = System.nanoTime();
        Call call = httpClient.newCall(request);
        // 注册取消回调后立即 enqueue：不等待、不休眠，也不占用业务请求线程。
        // 完成回调必须注销 registration，避免长生命周期取消令牌持有已结束的 Call。
        AutoCloseable registration = Objects.nonNull(cancellation) ? cancellation.onCancel(call::cancel) : null;
        CompletableFuture<HttpResponseData> result = new CompletableFuture<>();
        call.enqueue(new Callback() {
            /**
             * <p>处理异步传输失败回调。</p>
             *
             * @param ignored 未使用的回调参数
             * @param error 导致流失败的异常
             * @since 1.0.0
             */
            @Override
            public void onFailure(Call ignored, IOException error) {
                closeRegistration(registration);
                logFailure(requestId, request, startedAt, error);
                result.completeExceptionally(new HermesHttpException(
                        "HTTP request failed: " + error.getMessage(), error));
            }

            /**
             * <p>处理异步 HTTP 响应回调。</p>
             *
             * @param ignored 未使用的回调参数
             * @param response OkHttp 响应
             * @since 1.0.0
             */
            @Override
            public void onResponse(Call ignored, Response response) {
                // 在关闭 OkHttp Response 前读取不可变快照，使后续异步阶段可安全解析正文。
                try (Response completed = response) {
                    String body = Objects.nonNull(completed.body()) ? completed.body().string() : "";
                    logResponse(requestId, request, completed.code(), body, startedAt);
                    result.complete(new HttpResponseData(completed.code(), body));
                } catch (Exception error) {
                    logFailure(requestId, request, startedAt, error);
                    result.completeExceptionally(error);
                } finally {
                    closeRegistration(registration);
                }
            }
        });
        // Future.cancel 与业务取消令牌最终汇聚到同一个 Call.cancel。
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                call.cancel();
            }
        });
        return result;
    }

    private void closeRegistration(AutoCloseable registration) {
        if (registration == null) {
            return;
        }
        try {
            registration.close();
        } catch (Exception error) {
            log.debug("Failed to unregister HTTP cancellation callback: {}", error.getMessage());
        }
    }

    private <T> T executeList(Request request, TypeReference<T> typeRef) {
        return awaitFuture(executeResponseAsync(request, null).thenApply(response -> {
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.getStatusCode(), response.getBody());
            }
            try {
                return objectMapper.readValue(response.getBody(), typeRef);
            } catch (IOException error) {
                throw new HermesHttpException("Failed to parse response: " + error.getMessage(), error);
            }
        }));
    }

    /**
     * <p>等待异步 HTTP 结果并保持 Hermes 异常语义。</p>
     *
     * <p>仅供同步兼容 API 在调用线程等待异步结果，并保持 Hermes 运行时异常语义。</p>
     *
     * @param <T> 方法返回或异步承载的目标类型
     * @param future 待等待的异步结果
     * @return 异步计算完成的类型化结果
     * @since 1.0.0
     */
    protected <T> T awaitFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException error) {
            Throwable cause = Objects.nonNull(error.getCause()) ? error.getCause() : error;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new HermesHttpException("Async HTTP request failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * <p>HTTP 响应快照。</p>
     *
     * <p>在关闭 OkHttp Response 前保存状态码和响应体，供异步阶段安全消费。</p>
     *
     * @author <a href="https://github.com/loong10k">Loong Wan</a>
     * @since 1.0.0
     */
    private static final class HttpResponseData {
        /**
         * HTTP 响应状态码。
         */
        private final int statusCode;
        /**
         * 关闭底层响应前读取的响应体文本。
         */
        private final String body;

        private HttpResponseData(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getBody() {
            return body;
        }

        private boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    private long beginTrace(Request request) {
        // 原子序列只用于 JVM 内关联请求生命周期日志，不改变协议与重试语义。
        long requestId = REQUEST_SEQUENCE.incrementAndGet();
        log.debug("HTTP request started: requestId={}, method={}, url={}",
                requestId, request.method(), request.url());
        if (config.isDetailedLoggingEnabled()) {
            log.debug("HTTP request details: requestId={}, headers={}, body={}", requestId,
                    redactHeaders(request.headers()), requestBody(request));
        }
        return requestId;
    }

    private void logResponse(long requestId, Request request, int status, String body, long startedAt) {
        log.debug("HTTP request completed: requestId={}, method={}, url={}, status={}, bodyLength={}, elapsedMs={}",
                requestId, request.method(), request.url(), status, body.length(), elapsedMillis(startedAt));
        if (config.isDetailedLoggingEnabled()) {
            log.debug("HTTP response body: requestId={}, body={}", requestId, truncate(body));
        }
    }

    private void logFailure(long requestId, Request request, long startedAt, Exception error) {
        log.warn("HTTP request failed: requestId={}, method={}, url={}, elapsedMs={}, error={}",
                requestId, request.method(), request.url(), elapsedMillis(startedAt), error.getMessage());
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String requestBody(Request request) {
        if (Objects.isNull(request.body())) {
            return "";
        }
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return truncate(buffer.readUtf8());
        } catch (IOException error) {
            return "<unavailable:" + error.getMessage() + ">";
        }
    }

    private String truncate(String value) {
        int limit = Math.max(0, config.getMaxLoggedBodyLength());
        return value.length() <= limit ? value : value.substring(0, limit) + "...<truncated>";
    }

    private Headers redactHeaders(Headers headers) {
        Headers.Builder safe = headers.newBuilder();
        for (String name : headers.names()) {
            String lowerName = name.toLowerCase();
            if ("authorization".equals(lowerName) || lowerName.contains("token") || lowerName.contains("key")) {
                safe.set(name, "██");
            }
        }
        return safe.build();
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new HermesHttpException("Failed to serialize request body: " + e.getMessage(), e);
        }
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
        if (ownsHttpClient) {
            HermesOkHttpClientFactory.shutdown(httpClient);
        }
    }
}
