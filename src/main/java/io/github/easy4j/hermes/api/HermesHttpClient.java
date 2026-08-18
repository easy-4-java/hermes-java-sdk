package io.github.easy4j.hermes.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.*;
import io.github.easy4j.hermes.exception.HermesHttpException;
import kong.unirest.core.*;
import kong.unirest.modules.jackson.JacksonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hermes Server HTTP 客户端，封装 REST API。
 */
public class HermesHttpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HermesHttpClient.class);

    private final HermesClientConfig config;
    private final UnirestInstance unirest;

    public HermesHttpClient(HermesClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.unirest = new UnirestInstance(new Config()
                .connectTimeout(config.getConnectTimeoutMillis())
                .requestTimeout(config.getReadTimeoutMillis())
                .verifySsl(config.isVerifySsl())
                .setObjectMapper(new JacksonObjectMapper(mapper)));

        String apiKey = config.resolveApiKey();
        if (!apiKey.isEmpty()) {
            this.unirest.config().setDefaultHeader("Authorization", "Bearer " + apiKey);
        }
    }

    // ============================================================
    // Global / Health
    // ============================================================

    public HealthStatus health() { return get("/health", HealthStatus.class); }

    public HealthStatus healthDetailed() { return get("/health/detailed", HealthStatus.class); }

    public HealthStatus healthV1() { return get("/v1/health", HealthStatus.class); }

    // ============================================================
    // Chat Completion
    // ============================================================

    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        return post("/v1/chat/completions", request, ChatCompletionResponse.class);
    }

    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request, Map<String, String> headers) {
        return post("/v1/chat/completions", request, ChatCompletionResponse.class, headers);
    }

    // ============================================================
    // Responses API
    // ============================================================

    public ResponseResult createResponse(ResponseRequest request) {
        return post("/v1/responses", request, ResponseResult.class);
    }

    public ResponseResult createResponse(ResponseRequest request, Map<String, String> headers) {
        return post("/v1/responses", request, ResponseResult.class, headers);
    }

    public ResponseResult getResponse(String responseId) {
        return get("/v1/responses/" + responseId, ResponseResult.class);
    }

    public boolean deleteResponse(String responseId) {
        HttpResponse<String> resp = unirest.delete(url("/v1/responses/" + responseId)).asString();
        return resp.isSuccess();
    }

    // ============================================================
    // Models & Capabilities
    // ============================================================

    public ModelsResponse listModels() {
        return get("/v1/models", ModelsResponse.class);
    }

    public CapabilityInfo getCapabilities() { return get("/v1/capabilities", CapabilityInfo.class); }

    // ============================================================
    // Skills & Toolsets
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listSkills() {
        return getList("/v1/skills", new GenericType<List<Map<String, Object>>>() {});
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listToolsets() {
        return getList("/v1/toolsets", new GenericType<List<Map<String, Object>>>() {});
    }

    // ============================================================
    // Run
    // ============================================================

    public RunStatus createRun(RunCreateRequest request) {
        return post("/v1/runs", request, RunStatus.class);
    }

    public RunStatus getRun(String runId) { return get("/v1/runs/" + runId, RunStatus.class); }

    public void stopRun(String runId) {
        HttpResponse<String> resp = unirest.post(url("/v1/runs/" + runId + "/stop")).asString();
        if (!resp.isSuccess()) throw new HermesHttpException(resp.getStatus(), resp.getBody() != null ? resp.getBody() : "");
    }

    public Map<String, Object> approveRun(String runId, Map<String, Object> decision) {
        return postMap("/v1/runs/" + runId + "/approval", decision);
    }

    // ============================================================
    // Session
    // ============================================================

    public Session createSession(String title) {
        Map<String, Object> body = title != null ? Map.of("title", title) : Map.of();
        return post("/api/sessions", body, Session.class);
    }

    public List<Session> listSessions() {
        return getList("/api/sessions", new GenericType<List<Session>>() {});
    }

    public Session getSession(String id) { return get("/api/sessions/" + id, Session.class); }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSessionMessages(String id) {
        return getList("/api/sessions/" + id + "/messages",
                new GenericType<List<Map<String, Object>>>() {});
    }

    public Session forkSession(String id, String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (title != null) body.put("title", title);
        return post("/api/sessions/" + id + "/fork", body, Session.class);
    }

    public boolean deleteSession(String id) {
        HttpResponse<String> resp = unirest.delete(url("/api/sessions/" + id)).asString();
        return resp.isSuccess();
    }

    public ChatCompletionResponse sessionChat(String id, String input) {
        return post("/api/sessions/" + id + "/chat", Map.of("input", input), ChatCompletionResponse.class);
    }

    // ============================================================
    // Jobs
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listJobs() {
        return getList("/api/jobs", new GenericType<List<Map<String, Object>>>() {});
    }

    public Map<String, Object> createJob(Map<String, Object> job) {
        return postMap("/api/jobs", job);
    }

    public Map<String, Object> getJob(String jobId) {
        return get("/api/jobs/" + jobId, (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    public Map<String, Object> updateJob(String jobId, Map<String, Object> patch) {
        return postMap("/api/jobs/" + jobId, patch); // uses PATCH via POST compat
    }

    public boolean deleteJob(String jobId) {
        HttpResponse<String> resp = unirest.delete(url("/api/jobs/" + jobId)).asString();
        return resp.isSuccess();
    }

    public Map<String, Object> pauseJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/pause", Map.of());
    }

    public Map<String, Object> resumeJob(String jobId) {
        return postMap("/api/jobs/" + jobId + "/resume", Map.of());
    }

    public Map<String, Object> runJobNow(String jobId) {
        return postMap("/api/jobs/" + jobId + "/run", Map.of());
    }

    // ============================================================
    // Hermes-specific headers
    // ============================================================

    /** Build Hermes-specific request headers. */
    public static Map<String, String> hermesHeaders(String sessionKey, String sessionId, String messageChannel) {
        Map<String, String> h = new LinkedHashMap<>();
        if (sessionKey != null) h.put("X-Hermes-Session-Key", sessionKey);
        if (sessionId != null) h.put("X-Hermes-Session-Id", sessionId);
        if (messageChannel != null) h.put("X-Hermes-Message-Channel", messageChannel);
        return h.isEmpty() ? null : h;
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private String url(String path) {
        return config.getServerUrl() + path;
    }

    private <T> T get(String path, Class<T> type) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(type);
        checkResponse(resp);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private <T> T getList(String path, GenericType<T> genericType) {
        HttpResponse<T> resp = unirest.get(url(path)).asObject(genericType);
        checkResponse(resp);
        return resp.getBody();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        HttpResponse<T> resp = unirest.post(url(path))
                .header("Content-Type", "application/json").body(body).asObject(type);
        checkResponse(resp);
        return resp.getBody();
    }

    private <T> T post(String path, Object body, Class<T> type, Map<String, String> headers) {
        HttpRequestWithBody req = unirest.post(url(path))
                .header("Content-Type", "application/json");
        if (headers != null) headers.forEach((k, v) -> { if (v != null) req.header(k, v); });
        HttpResponse<T> resp = req.body(body).asObject(type);
        checkResponse(resp);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postMap(String path, Object body) {
        HttpResponse<Map> resp = unirest.post(url(path))
                .header("Content-Type", "application/json").body(body).asObject(Map.class);
        checkResponse(resp);
        return resp.getBody();
    }

    private <T> void checkResponse(HttpResponse<T> resp) {
        if (!resp.isSuccess()) {
            throw new HermesHttpException(resp.getStatus(),
                    resp.getBody() != null ? resp.getBody().toString() : "");
        }
    }

    @Override
    public void close() { unirest.close(); }
}
