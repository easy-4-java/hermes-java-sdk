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
import java.util.concurrent.atomic.AtomicLong;
import okio.Buffer;

/**
 * Hermes Server HTTP 客户端，封装 REST API。
 * <p>基于 OkHttp，支持外部传入 {@link OkHttpClient}（复用别的插件实例）。</p>
 */
@Slf4j
@SuppressWarnings("unchecked")
public class HermesHttpClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final AtomicLong REQUEST_SEQUENCE = new AtomicLong();

    private final HermesHttpClientConfig config;
    /**
     * -- GETTER --
     *  暴露 ObjectMapper 供外部复用。
     */
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

    public HealthStatus health() { return get(PATH_HEALTH, HealthStatus.class); }
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
        return post(PATH_CHAT_COMPLETIONS, request, ChatResponse.class, headers, cancellation);
    }

    // ============================================================
    // Responses API
    // ============================================================

    public ResponseResult createResponse(ResponseRequest request) {
        return post(PATH_RESPONSES, request, ResponseResult.class);
    }

    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
        return post(PATH_RESPONSES, request, ResponseResult.class, headers);
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

    public ModelsResponse listModels() { return get(PATH_MODELS, ModelsResponse.class); }

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
        Request request = authedRequest(url(PATH_RUNS + "/" + runId + "/stop"))
                .post(RequestBody.create(new byte[0], null)).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new HermesHttpException(response.code(), body);
            }
        } catch (IOException e) {
            throw new HermesHttpException("stopRun failed: " + e.getMessage(), e);
        }
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), body);
            }
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new HermesHttpException("getJob failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId))
                .patch(RequestBody.create(toJson(patch), JSON)).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), body);
            }
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new HermesHttpException("updateJob failed: " + e.getMessage(), e);
        }
    }

    public boolean deleteJob(String jobId) {
        Request request = authedRequest(url(PATH_JOBS + "/" + jobId)).delete().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                log.warn("deleteJob {} failed: {}", jobId, response.code());
            }
            return response.isSuccessful();
        } catch (IOException e) {
            throw new HermesHttpException("deleteJob failed: " + e.getMessage(), e);
        }
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
        Request request = authedRequest(url(path)).get().build();
        return execute(request, type);
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

    private <T> T post(String path, Object body, Class<T> type, Map<String, String> headers,
                       HttpCallCancellation cancellation) {
        Request.Builder builder = authedRequest(url(path));
        if (headers != null) {
            headers.forEach((k, v) -> { if (k != null && v != null) builder.header(k, v); });
        }
        Request request = builder.post(RequestBody.create(toJson(body), JSON)).build();
        return execute(request, type, cancellation);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postMap(String path, Object body) {
        Request request = authedRequest(url(path))
                .post(RequestBody.create(toJson(body), JSON)).build();
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new HermesHttpException("postMap failed: " + e.getMessage(), e);
        }
    }

    private boolean deleteBoolean(String path) {
        Request request = authedRequest(url(path)).delete().build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new HermesHttpException("DELETE failed: " + e.getMessage(), e);
        }
    }

    private <T> T execute(Request request, Class<T> type) {
        return execute(request, type, null);
    }

    private <T> T execute(Request request, Class<T> type, HttpCallCancellation cancellation) {
        long requestId = beginTrace(request);
        long startedAt = System.nanoTime();
        Call call = httpClient.newCall(request);
        AutoCloseable registration = cancellation != null ? cancellation.onCancel(call::cancel) : null;
        try (Response response = call.execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            logResponse(requestId, request, response.code(), respBody, startedAt);
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, type);
        } catch (IOException e) {
            logFailure(requestId, request, startedAt, e);
            throw new HermesHttpException("HTTP request failed: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logFailure(requestId, request, startedAt, e);
            throw e;
        } finally {
            closeRegistration(registration);
        }
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
        long requestId = beginTrace(request);
        long startedAt = System.nanoTime();
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            logResponse(requestId, request, response.code(), respBody, startedAt);
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, typeRef);
        } catch (IOException e) {
            logFailure(requestId, request, startedAt, e);
            throw new HermesHttpException("HTTP request failed: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logFailure(requestId, request, startedAt, e);
            throw e;
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
