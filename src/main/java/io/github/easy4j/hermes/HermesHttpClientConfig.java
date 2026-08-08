package io.github.easy4j.hermes;

import io.github.easy4j.hermes.api.HermesApiConstants;
import lombok.Data;

import java.util.Objects;

/**
 * Hermes API Server HTTP 客户端配置。
 * <p>
 * 涵盖 Server 根地址、Bearer 鉴权、TLS、HTTP 超时及默认模型等网络相关设置。
 * </p>
 */
@Data
public class HermesHttpClientConfig {

    /** 对话响应模式，默认保持兼容的完整响应模式。 */
    private HttpResponseMode mode = HttpResponseMode.BLOCKING;

    /**
     * 是否启用 HTTP API 功能。
     * <p>为 false 时跳过 HTTP 客户端初始化和检查。</p>
     */
    private boolean enabled = true;

    /**
     * 启动时是否探测 HTTP 服务可用性。
     */
    private boolean startupCheckEnabled = false;

    /**
     * HTTP 服务不可用时是否快速失败。
     */
    private boolean failFastOnUnavailable = false;

    /**
     * Hermes API Server 根地址，例如 {@code http://localhost:8642}。
     */
    private String serverUrl = HermesApiConstants.DEFAULT_SERVER_URL;

    /**
     * Bearer token（对应 {@code API_SERVER_KEY}）。
     * <p>为空时不使用 Bearer Auth。</p>
     */
    private String apiKey;

    /**
     * 连接超时（毫秒）。
     */
    private int connectTimeoutMillis = HermesApiConstants.DEFAULT_CONNECT_TIMEOUT_MS;

    /**
     * 读取超时（毫秒）。
     * <p>Hermes 的 run 请求可能耗时较长，建议设置较大值。</p>
     */
    private int readTimeoutMillis = HermesApiConstants.DEFAULT_READ_TIMEOUT_MS;

    /** 写入超时（毫秒）。 */
    private int writeTimeoutMillis = 10_000;

    /** 整个调用超时（毫秒）；0 表示不额外限制。 */
    private int callTimeoutMillis;

    /** 连接池最大空闲连接数。 */
    private int maxIdleConnections = 32;

    /** 空闲连接保活时间（毫秒）。 */
    private long keepAliveDurationMillis = 300_000L;

    /** 异步请求最大并发数。 */
    private int maxRequests = 128;

    /** 单主机异步请求最大并发数。 */
    private int maxRequestsPerHost = 64;

    /** 流式响应消费线程池核心线程数。 */
    private int streamCorePoolSize = 64;

    /** SSE 消费线程池最大线程数。 */
    private int streamMaxPoolSize = 64;

    /** SSE 消费线程池有界队列容量。 */
    private int streamQueueCapacity = 128;

    /** SSE 消费线程空闲保活时间（毫秒）。 */
    private long streamKeepAliveMillis = 60_000L;

    /** 单个流式订阅的事件缓存上限。 */
    private int streamEventQueueCapacity = 1_024;

    /** 遇到失效连接等传输故障时是否允许 OkHttp 自动恢复。 */
    private boolean retryOnConnectionFailure = true;

    /** @deprecated 使用 {@link #getStreamCorePoolSize()}。 */
    @Deprecated
    public int getSseCorePoolSize() {
        return streamCorePoolSize;
    }

    /** @deprecated 使用 {@link #setStreamCorePoolSize(int)}。 */
    @Deprecated
    public void setSseCorePoolSize(int value) {
        this.streamCorePoolSize = value;
    }

    /** @deprecated 使用 {@link #getStreamMaxPoolSize()}。 */
    @Deprecated
    public int getSseMaxPoolSize() {
        return streamMaxPoolSize;
    }

    /** @deprecated 使用 {@link #setStreamMaxPoolSize(int)}。 */
    @Deprecated
    public void setSseMaxPoolSize(int value) {
        this.streamMaxPoolSize = value;
    }

    /** @deprecated 使用 {@link #getStreamQueueCapacity()}。 */
    @Deprecated
    public int getSseQueueCapacity() {
        return streamQueueCapacity;
    }

    /** @deprecated 使用 {@link #setStreamQueueCapacity(int)}。 */
    @Deprecated
    public void setSseQueueCapacity(int value) {
        this.streamQueueCapacity = value;
    }

    /** @deprecated 使用 {@link #getStreamKeepAliveMillis()}。 */
    @Deprecated
    public long getSseKeepAliveMillis() {
        return streamKeepAliveMillis;
    }

    /** @deprecated 使用 {@link #setStreamKeepAliveMillis(long)}。 */
    @Deprecated
    public void setSseKeepAliveMillis(long value) {
        this.streamKeepAliveMillis = value;
    }

    /** @deprecated 使用 {@link #getStreamEventQueueCapacity()}。 */
    @Deprecated
    public int getSseEventQueueCapacity() {
        return streamEventQueueCapacity;
    }

    /** @deprecated 使用 {@link #setStreamEventQueueCapacity(int)}。 */
    @Deprecated
    public void setSseEventQueueCapacity(int value) {
        this.streamEventQueueCapacity = value;
    }

    /**
     * 是否校验 HTTPS 证书；为 false 时关闭校验（仅建议开发环境）。
     */
    private boolean verifySsl = true;

    /**
     * 默认使用的模型，例如 {@code hermes-agent}。
     * <p>为空时使用 Hermes 服务端配置的默认模型。</p>
     */
    private String defaultModel = HermesApiConstants.DEFAULT_MODEL;

    /**
     * 默认指令（system prompt）。
     * <p>为空时使用 Hermes 服务端配置的默认指令。</p>
     */
    private String defaultInstructions;

    /**
     * 默认 provider 名称。
     * <p>为空时使用 Hermes 服务端配置的默认 provider。</p>
     */
    private String defaultProvider;

    /**
     * 解析用于 Bearer Auth 的 API key。
     *
     * @return apiKey 非空则用之，否则空字符串
     */
    public String resolveApiKey() {
        return Objects.toString(apiKey, "");
    }
}
