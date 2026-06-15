package io.github.hiwepy.hermes.api;

/**
 * Hermes API Server constants — paths, header names, defaults.
 */
public final class HermesApiConstants {

    private HermesApiConstants() {}

    // ============================================================
    // Defaults
    // ============================================================

    public static final String DEFAULT_SERVER_URL = "http://localhost:8642";
    public static final String DEFAULT_MODEL = "hermes-agent";
    public static final String DEFAULT_EXECUTABLE = "hermes";
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 15_000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 300_000;
    public static final int DEFAULT_LOCAL_TIMEOUT_SECONDS = 300;
    public static final int DEFAULT_PROBE_TIMEOUT_SECONDS = 5;

    // ============================================================
    // HTTP paths
    // ============================================================

    public static final String PATH_HEALTH = "/health";
    public static final String PATH_HEALTH_DETAILED = "/health/detailed";
    public static final String PATH_V1_HEALTH = "/v1/health";

    public static final String PATH_CHAT_COMPLETIONS = "/v1/chat/completions";
    public static final String PATH_RESPONSES = "/v1/responses";
    public static final String PATH_MODELS = "/v1/models";
    public static final String PATH_CAPABILITIES = "/v1/capabilities";
    public static final String PATH_SKILLS = "/v1/skills";
    public static final String PATH_TOOLSETS = "/v1/toolsets";

    public static final String PATH_RUNS = "/v1/runs";
    public static final String PATH_SESSIONS = "/api/sessions";
    public static final String PATH_JOBS = "/api/jobs";

    // ============================================================
    // Hermes-specific HTTP headers
    // ============================================================

    public static final String HEADER_SESSION_KEY = "X-Hermes-Session-Key";
    public static final String HEADER_SESSION_ID = "X-Hermes-Session-Id";
    public static final String HEADER_MESSAGE_CHANNEL = "X-Hermes-Message-Channel";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    // ============================================================
    // Standard HTTP
    // ============================================================

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";

    public static final String AUTH_BEARER_PREFIX = "Bearer ";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String MEDIA_TYPE_SSE = "text/event-stream";
    public static final String CACHE_NO_CACHE = "no-cache";

    // ============================================================
    // SSE protocol
    // ============================================================

    public static final String SSE_DATA_PREFIX = "data: ";
    public static final String SSE_EVENT_PREFIX = "event: ";
    public static final String SSE_DONE_MARKER = "[DONE]";
    public static final String SSE_EVENT_TOOL_PROGRESS = "hermes.tool.progress";

    // ============================================================
    // Run status values
    // ============================================================

    public static final String STATUS_STARTED = "started";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_STOPPING = "stopping";

    // ============================================================
    // Chat completion SSE event types
    // ============================================================

    public static final String EVENT_CHAT_COMPLETION_CHUNK = "chat.completion.chunk";
    public static final String EVENT_RESPONSE_CREATED = "response.created";
    public static final String EVENT_RESPONSE_OUTPUT_TEXT_DELTA = "response.output_text.delta";
    public static final String EVENT_RESPONSE_OUTPUT_ITEM_ADDED = "response.output_item.added";
    public static final String EVENT_RESPONSE_OUTPUT_ITEM_DONE = "response.output_item.done";
    public static final String EVENT_RESPONSE_COMPLETED = "response.completed";
    public static final String EVENT_RESPONSE_FAILED = "response.failed";

    // ============================================================
    // Session SSE event types
    // ============================================================

    public static final String EVENT_ASSISTANT_DELTA = "assistant.delta";
    public static final String EVENT_TOOL_STARTED = "tool.started";
    public static final String EVENT_TOOL_COMPLETED = "tool.completed";
    public static final String EVENT_RUN_COMPLETED = "run.completed";

    // ============================================================
    // Response output item types
    // ============================================================

    public static final String OUTPUT_ITEM_MESSAGE = "message";
    public static final String OUTPUT_ITEM_FUNCTION_CALL = "function_call";
    public static final String OUTPUT_ITEM_FUNCTION_CALL_OUTPUT = "function_call_output";
    public static final String OUTPUT_TEXT_TYPE = "output_text";
}
