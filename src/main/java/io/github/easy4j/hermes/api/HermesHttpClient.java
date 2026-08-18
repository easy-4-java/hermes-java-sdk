package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesClientConfig;
import static io.github.easy4j.hermes.api.HermesApiConstants.*;
import io.github.easy4j.hermes.api.model.*;
import io.github.easy4j.hermes.exception.HermesHttpException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hermes Server HTTP 客户端，封装 REST API。
 * <p>基于 OkHttp，支持外部传入 {@link OkHttpClient}（复用别的插件实例）。</p>
 */
@Slf4j
public class HermesHttpClient implements AutoCloseable {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final HermesClientConfig config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public HermesHttpClient(HermesClientConfig config) {
        this(config, null, null);
    }

    public HermesHttpClient(HermesClientConfig config, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.objectMapper = Objects.isNull(objectMapper) ? new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) : objectMapper;
        this.httpClient = Objects.isNull(httpClient) ? buildOkHttpClient(config) : httpClient;
    }

    private static OkHttpClient buildOkHttpClient(HermesClientConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS);
        if (!config.isVerifySsl()) {
            builder.hostnameVerifier((hostname, session) -> true);
        }
        return builder.build();
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

    public ModelsResponse.ModelData getModel(String modelId) {
        try {
            return get(PATH_MODELS + "/" + java.net.URLEncoder.encode(modelId, "UTF-8"),
                    ModelsResponse.ModelData.class);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new java.io.UncheckedIOException(e);
        }
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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

    /**
     * 暴露 ObjectMapper 供外部复用。
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getServerUrl() + path;
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
        Request.Builder builder = authedRequest(url(path));
        if (headers != null) {
            headers.forEach((k, v) -> { if (k != null && v != null) builder.header(k, v); });
        }
        Request request = builder.post(RequestBody.create(toJson(body), JSON)).build();
        return execute(request, type);
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
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, type);
        } catch (IOException e) {
            throw new HermesHttpException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private <T> T executeList(Request request, TypeReference<T> typeRef) {
        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new HermesHttpException(response.code(), respBody);
            }
            return objectMapper.readValue(respBody, typeRef);
        } catch (IOException e) {
            throw new HermesHttpException("HTTP request failed: " + e.getMessage(), e);
        }
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
        // 外部传入的 OkHttpClient 不关闭；自建的也不主动关闭（OkHttpClient 内部管理连接池）
    }
}
