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
 * Hermes Server HTTP 客户端，封装 REST API。
 * <p>基于 OkHttp，支持外部传入 {@link OkHttpClient}（复用别的插件实例）。</p>
 */
@Slf4j
public class HermesHttpClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final AtomicLong REQUEST_SEQUENCE = new AtomicLong();

    private final HermesHttpClientConfig config;
    @Getter
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final boolean ownsHttpClient;

    public HermesHttpClient(HermesHttpClientConfig config) {
        this(config, null, HermesOkHttpClientFactory.create(config), true);
    }

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

    public HealthStatus health() { return awaitFuture(healthAsync()); }
    public CompletableFuture<HealthStatus> healthAsync() { return getAsync(PATH_HEALTH, HealthStatus.class); }
    public HealthStatus healthDetailed() { return get(PATH_HEALTH_DETAILED, HealthStatus.class); }
    public HealthStatus healthV1() { return get(PATH_V1_HEALTH, HealthStatus.class); }

    // ============================================================
    // Chat Completion
    // ============================================================

    public ChatResponse chatCompletion(ChatRequest request) {
        return post(PATH_CHAT_COMPLETIONS, request, ChatResponse.class);
    }

    public ChatResponse chatCompletion(ChatRequest request, Map<String, String> headers) {
        return post(PATH_CHAT_COMPLETIONS, request, ChatResponse.class, headers);
    }

    public ChatResponse chatCompletion(ChatRequest request, Map<String, String> headers,
                                       HttpCallCancellation cancellation) {
        return awaitFuture(chatCompletionAsync(request, headers, cancellation));
    }

    /** 异步发送 Chat Completion 请求。 */
    public CompletableFuture<ChatResponse> chatCompletionAsync(ChatRequest request,
                                                               Map<String, String> headers,
                                                               HttpCallCancellation cancellation) {
        return postAsync(PATH_CHAT_COMPLETIONS, request, ChatResponse.class, headers, cancellation);
    }

    public CompletableFuture<ChatResponse> chatCompletionAsync(ChatRequest request) {
        return chatCompletionAsync(request, null, null);
    }

    // ============================================================
    // Responses API
    // ============================================================

    public ResponseResult createResponse(ResponseRequest request) {
        return awaitFuture(createResponseAsync(request));
    }

    /** 异步创建 Responses API 响应。 */
    public CompletableFuture<ResponseResult> createResponseAsync(ResponseRequest request) {
        return postAsync(PATH_RESPONSES, request, ResponseResult.class, null, null);
    }

    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
        return post(PATH_RESPONSES, request, ResponseResult.class, headers);
    }

    public CompletableFuture<ResponseResult> createResponseAsync(ResponseRequest request,
                                                                 Map<String, String> headers) {
        return postAsync(PATH_RESPONSES, request, ResponseResult.class, headers, null);
    }

    public ResponseResult getResponse(String responseId) {
        return get("/v1/responses/" + responseId, ResponseResult.class);
    }

    public boolean deleteResponse(String responseId) {
        return deleteBoolean(PATH_RESPONSES + "/" + responseId);
    }

    // ============================================================
    // Models & Capabilities
    // ============================================================

    public ModelsResponse listModels() { return awaitFuture(listModelsAsync()); }
    public CompletableFuture<ModelsResponse> listModelsAsync() { return getAsync(PATH_MODELS, ModelsResponse.class); }

    private static String encodePathSegment(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public ModelsResponse.ModelData getModel(String modelId) {
        return get(PATH_MODELS + "/" + encodePathSegment(modelId),
                ModelsResponse.ModelData.class);
    }

    public CapabilityInfo getCapabilities() { return get(PATH_CAPABILITIES, CapabilityInfo.class); }

    // ============================================================
    // Skills & Toolsets
    // ============================================================

    public List<Map<String, Object>> listSkills() {
        return getList(PATH_SKILLS, new TypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> listToolsets() {
        return getList(PATH_TOOLSETS, new TypeReference<List<Map<String, Object>>>() {});
    }

    // ============================================================
    // Run
    // ============================================================

    public RunStatus createRun(RunCreateRequest request) {
        return post(PATH_RUNS, request, RunStatus.class);
    }

    public RunStatus getRun(String runId) { return get("/v1/runs/" + runId, RunStatus.class); }

    public void stopRun(String runId) {
        awaitFuture(stopRunAsync(runId));
    }

    /** 异步停止运行。 */
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

    public Map<String, Object> approveRun(String runId, Map<String, Object> decision) {
        return postMap(PATH_RUNS + "/" + runId + "/approval", decision);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        Map<String, Object> body = title != null ? Collections.singletonMap("title", title) : Collections.emptyMap();
        return post(PATH_SESSIONS, body, Session.class);
    }

    public List<Session> listSessions() {
        return getList(PATH_SESSIONS, new TypeReference<List<Session>>() {});
    }

    public List<Session> listSessions(Integer limit, Integer offset, String source, Boolean includeChildren) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(url(PATH_SESSIONS)).newBuilder();
        if (limit != null) urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        if (offset != null) urlBuilder.addQueryParameter("offset", String.valueOf(offset));
        if (source != null) urlBuilder.addQueryParameter("source", source);
        if (includeChildren != null) urlBuilder.addQueryParameter("include_children", String.valueOf(includeChildren));
        Request request = authedRequest(urlBuilder.build().toString()).get().build();
        return executeList(request, new TypeReference<List<Session>>() {});
    }

    public Session getSession(String id) { return get(PATH_SESSIONS + "/" + id, Session.class); }

    public List<Map<String, Object>> getSessionMessages(String id) {
        return getList(PATH_SESSIONS + "/" + id + "/messages",
                new TypeReference<List<Map<String, Object>>>() {});
    }

    public Session forkSession(String id, String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (title != null) body.put("title", title);
        return post(PATH_SESSIONS + "/" + id + "/fork", body, Session.class);
    }

    public boolean deleteSession(String id) {
        return deleteBoolean(PATH_SESSIONS + "/" + id);
    }

    public Session updateSession(String id, Map<String, Object> patch) {
        Request request = authedRequest(url(PATH_SESSIONS + "/" + id))
                .patch(RequestBody.create(toJson(patch), JSON)).build();
        return execute(request, Session.class);
    }

    public ChatResponse sessionChat(String id, String input) {
        return post(PATH_SESSIONS + "/" + id + "/chat", Collections.singletonMap("input", input), ChatResponse.class);
    }

    // ============================================================
    // Jobs
    // ============================================================

    public List<Map<String, Object>> listJobs() {
        return getList(PATH_JOBS, new TypeReference<List<Map<String, Object>>>() {});
    }

    public Map<String, Object> createJob(Map<String, Object> job) {
        return postMap(PATH_JOBS, job);
    }

    public Map<String, Object> getJob(String jobId) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId)).get().build();
        return executeList(request, new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId))
                .patch(RequestBody.create(toJson(patch), JSON)).build();
        return executeList(request, new TypeReference<Map<String, Object>>() {});
    }

    public boolean deleteJob(String jobId) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId)).delete().build();
        return awaitFuture(executeResponseAsync(request, null).thenApply(response -> {
            if (!response.isSuccessful() && response.getStatusCode() != 404) {
                log.warn("deleteJob {} failed: {}", jobId, response.getStatusCode());
            }
            return response.isSuccessful();
        }));
    }

    public Map<String, Object> pauseJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/pause", Collections.emptyMap());
    }

    public Map<String, Object> resumeJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/resume", Collections.emptyMap());
    }

    public Map<String, Object> runJobNow(String jobId) {
        return postMap("/api/jobs/" + jobId + "/run", Collections.emptyMap());
    }

    // ============================================================
    // Hermes-specific headers
    // ============================================================

    public static Map<String, String> hermesHeaders(String sessionKey, String sessionId, String messageChannel) {
        Map<String, String> h = new LinkedHashMap<>();
        if (sessionKey != null) h.put(HEADER_SESSION_KEY, sessionKey);
        if (sessionId != null) h.put(HEADER_SESSION_ID, sessionId);
        if (messageChannel != null) h.put(HEADER_MESSAGE_CHANNEL, messageChannel);
        return h;
    }

    /**
     * 暴露 OkHttpClient 供 SSE 客户端复用。
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

    /** 使用 OkHttp enqueue 异步执行并反序列化对象。 */
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
        AutoCloseable registration = Objects.nonNull(cancellation) ? cancellation.onCancel(call::cancel) : null;
        CompletableFuture<HttpResponseData> result = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call ignored, IOException error) {
                closeRegistration(registration);
                logFailure(requestId, request, startedAt, error);
                result.completeExceptionally(new HermesHttpException(
                        "HTTP request failed: " + error.getMessage(), error));
            }

            @Override
            public void onResponse(Call ignored, Response response) {
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

    private static final class HttpResponseData {
        private final int statusCode;
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

    @Override
    public void close() {
        if (ownsHttpClient) {
            HermesOkHttpClientFactory.shutdown(httpClient);
        }
    }
}
