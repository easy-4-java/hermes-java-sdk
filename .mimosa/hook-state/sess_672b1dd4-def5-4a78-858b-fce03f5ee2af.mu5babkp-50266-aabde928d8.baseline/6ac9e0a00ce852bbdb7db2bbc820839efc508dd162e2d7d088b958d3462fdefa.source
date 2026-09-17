package io.github.easy4j.hermes.api;
/**
 * <p>Hermes HTTP 与 SSE 协议常量。</p>
 *
 * <p>集中维护服务端路径、请求头、媒体类型、默认值和超时，避免协议字面量分散。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public final class HermesApiConstants {

    private HermesApiConstants() {}

    // ============================================================
    // Defaults
    // ============================================================

    /**
     * 本地 Hermes Server 的默认根地址。
     */
    public static final String DEFAULT_SERVER_URL = "http://localhost:8642";
    /**
     * 请求未指定模型时使用的 Hermes 默认模型名称。
     */
    public static final String DEFAULT_MODEL = "hermes-agent";
    /**
     * 从系统 PATH 查找本地 CLI 时使用的默认可执行文件名。
     */
    public static final String DEFAULT_EXECUTABLE = "hermes";
    /**
     * 默认连接超时，单位为毫秒。
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2_000;
    /**
     * 普通 HTTP 响应的默认读取超时，单位为毫秒。
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 120_000;
    /**
     * 本地 CLI 命令的默认执行超时，单位为秒。
     */
    public static final int DEFAULT_LOCAL_TIMEOUT_SECONDS = 300;
    /**
     * 本地 CLI 版本探测的默认超时，单位为秒。
     */
    public static final int DEFAULT_PROBE_TIMEOUT_SECONDS = 5;

    // ============================================================
    // HTTP paths
    // ============================================================

    /**
     * 查询服务基础存活状态的端点。
     */
    public static final String PATH_HEALTH = "/health";
    /**
     * 查询组件级详细健康状态的端点。
     */
    public static final String PATH_HEALTH_DETAILED = "/health/detailed";
    /**
     * 查询 OpenAI 兼容 V1 服务健康状态的端点。
     */
    public static final String PATH_V1_HEALTH = "/v1/health";

    /**
     * 创建聊天补全响应或事件流的端点。
     */
    public static final String PATH_CHAT_COMPLETIONS = "/v1/chat/completions";
    /**
     * 创建和管理 Responses API 响应的根端点。
     */
    public static final String PATH_RESPONSES = "/v1/responses";
    /**
     * 列出和查询可用模型的根端点。
     */
    public static final String PATH_MODELS = "/v1/models";
    /**
     * 查询服务平台、鉴权和功能能力的端点。
     */
    public static final String PATH_CAPABILITIES = "/v1/capabilities";
    /**
     * 列出服务端已加载技能的端点。
     */
    public static final String PATH_SKILLS = "/v1/skills";
    /**
     * 列出服务端可用工具集的端点。
     */
    public static final String PATH_TOOLSETS = "/v1/toolsets";

    /**
     * 创建和管理 Agent Run 的根端点。
     */
    public static final String PATH_RUNS = "/v1/runs";
    /**
     * 创建和管理持久化会话的根端点。
     */
    public static final String PATH_SESSIONS = "/api/sessions";
    /**
     * 创建和管理后台任务的根端点。
     */
    public static final String PATH_JOBS = "/api/jobs";

    // ============================================================
    // Hermes-specific HTTP headers
    // ============================================================

    /**
     * 传递业务会话键的 Hermes 扩展请求头。
     */
    public static final String HEADER_SESSION_KEY = "X-Hermes-Session-Key";
    /**
     * 传递服务端会话标识的 Hermes 扩展请求头。
     */
    public static final String HEADER_SESSION_ID = "X-Hermes-Session-Id";
    /**
     * 传递消息来源通道的 Hermes 扩展请求头。
     */
    public static final String HEADER_MESSAGE_CHANNEL = "X-Hermes-Message-Channel";
    /**
     * 防止重试请求重复产生副作用的幂等键请求头。
     */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    // ============================================================
    // Standard HTTP
    // ============================================================

    /**
     * 携带 Bearer 凭证的标准鉴权请求头。
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    /**
     * 声明请求体媒体类型的标准请求头。
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    /**
     * 声明客户端可接收媒体类型的标准请求头。
     */
    public static final String HEADER_ACCEPT = "Accept";
    /**
     * 控制中间代理和服务端缓存行为的标准请求头。
     */
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /**
     * Authorization 请求头中 Bearer token 的固定前缀。
     */
    public static final String AUTH_BEARER_PREFIX = "Bearer ";
    /**
     * JSON 请求体使用的 Content-Type 值。
     */
    public static final String CONTENT_TYPE_JSON = "application/json";
    /**
     * SSE 连接使用的 Accept 媒体类型。
     */
    public static final String MEDIA_TYPE_SSE = "text/event-stream";
    /**
     * 要求 SSE 代理和服务端不缓存事件流的 Cache-Control 值。
     */
    public static final String CACHE_NO_CACHE = "no-cache";

    // ============================================================
    // SSE protocol
    // ============================================================

    /**
     * SSE 文本协议中的 data 字段前缀。
     */
    public static final String SSE_DATA_PREFIX = "data: ";
    /**
     * SSE 文本协议中的 event 字段前缀。
     */
    public static final String SSE_EVENT_PREFIX = "event: ";
    /**
     * 服务端表示事件流正常结束的终止标记。
     */
    public static final String SSE_DONE_MARKER = "[DONE]";
    /**
     * 工具执行进度事件类型。
     */
    public static final String SSE_EVENT_TOOL_PROGRESS = "hermes.tool.progress";

    // ============================================================
    // Run status values
    // ============================================================

    /**
     * Agent Run 已开始执行。
     */
    public static final String STATUS_STARTED = "started";
    /**
     * Agent Run 已成功完成。
     */
    public static final String STATUS_COMPLETED = "completed";
    /**
     * Agent Run 因错误终止。
     */
    public static final String STATUS_FAILED = "failed";
    /**
     * Agent Run 已被调用方取消。
     */
    public static final String STATUS_CANCELLED = "cancelled";
    /**
     * Agent Run 正在响应停止请求。
     */
    public static final String STATUS_STOPPING = "stopping";

    // ============================================================
    // Chat completion SSE event types
    // ============================================================

    /**
     * Chat Completion 返回一个增量分片的事件类型。
     */
    public static final String EVENT_CHAT_COMPLETION_CHUNK = "chat.completion.chunk";
    /**
     * Responses API 已创建响应的事件类型。
     */
    public static final String EVENT_RESPONSE_CREATED = "response.created";
    /**
     * Responses API 返回文本增量的事件类型。
     */
    public static final String EVENT_RESPONSE_OUTPUT_TEXT_DELTA = "response.output_text.delta";
    /**
     * Responses API 新增输出项的事件类型。
     */
    public static final String EVENT_RESPONSE_OUTPUT_ITEM_ADDED = "response.output_item.added";
    /**
     * Responses API 完成一个输出项的事件类型。
     */
    public static final String EVENT_RESPONSE_OUTPUT_ITEM_DONE = "response.output_item.done";
    /**
     * Responses API 响应正常完成的事件类型。
     */
    public static final String EVENT_RESPONSE_COMPLETED = "response.completed";
    /**
     * Responses API 响应失败的事件类型。
     */
    public static final String EVENT_RESPONSE_FAILED = "response.failed";

    // ============================================================
    // Session SSE event types
    // ============================================================

    /**
     * 会话中助手返回文本增量的事件类型。
     */
    public static final String EVENT_ASSISTANT_DELTA = "assistant.delta";
    /**
     * 会话中工具开始执行的事件类型。
     */
    public static final String EVENT_TOOL_STARTED = "tool.started";
    /**
     * 会话中工具执行完成的事件类型。
     */
    public static final String EVENT_TOOL_COMPLETED = "tool.completed";
    /**
     * 会话关联 Run 完成的事件类型。
     */
    public static final String EVENT_RUN_COMPLETED = "run.completed";

    // ============================================================
    // Response output item types
    // ============================================================

    /**
     * 表示助手消息的 Responses API 输出项类型。
     */
    public static final String OUTPUT_ITEM_MESSAGE = "message";
    /**
     * 表示待执行函数调用的 Responses API 输出项类型。
     */
    public static final String OUTPUT_ITEM_FUNCTION_CALL = "function_call";
    /**
     * 表示函数执行结果的 Responses API 输出项类型。
     */
    public static final String OUTPUT_ITEM_FUNCTION_CALL_OUTPUT = "function_call_output";
    /**
     * Responses API 文本输出内容类型。
     */
    public static final String OUTPUT_TEXT_TYPE = "output_text";
}
