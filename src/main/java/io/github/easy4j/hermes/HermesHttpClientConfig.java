package io.github.easy4j.hermes;
import io.github.easy4j.hermes.api.HermesApiConstants;
import lombok.Data;

import java.util.Objects;

/**
 * <p>Hermes HTTP 与 SSE 传输配置。</p>
 *
 * <p>定义端点、鉴权、超时、连接池、并发、重连、日志和默认模型等网络行为。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class HermesHttpClientConfig {

    /** HTTP 与 SSE 通道共享的调试配置。 */
    private final HermesDebugConfig debug;

    /** 使用默认关闭的调试配置创建 HTTP 配置。 */
    public HermesHttpClientConfig() {
        this(new HermesDebugConfig());
    }

    /**
     * 使用客户端级共享调试配置创建 HTTP 配置。
     *
     * @param debug 客户端级调试配置
     */
    public HermesHttpClientConfig(HermesDebugConfig debug) {
        this.debug = Objects.requireNonNull(debug, "debug");
    }

    /**
     * 对话响应模式，默认返回完整响应。
     */
    private HttpResponseMode mode = HttpResponseMode.BLOCKING;

    /**
     * 是否启用对应客户端通道。
     */
    private boolean enabled = true;

    /**
     * 是否在客户端初始化时执行可用性探测。
     */
    private boolean startupCheckEnabled = false;

    /**
     * 探测不可用时是否立即抛出异常。
     */
    private boolean failFastOnUnavailable = false;

    /**
     * Hermes Server 根地址，不包含具体 API 路径。
     */
    private String baseUrl = HermesApiConstants.DEFAULT_SERVER_URL;

    /**
     * Bearer 鉴权密钥；为空时不发送 Authorization 请求头。
     */
    private String apiKey;

    /**
     * 建立连接的超时时间，单位为毫秒。
     */
    private int connectTimeoutMillis = HermesApiConstants.DEFAULT_CONNECT_TIMEOUT_MS;

    /**
     * 读取响应的超时时间，单位为毫秒；SSE 会派生为无限读取。
     */
    private int readTimeoutMillis = HermesApiConstants.DEFAULT_READ_TIMEOUT_MS;

    /**
     * 写入请求体的超时时间，单位为毫秒。
     */
    private int writeTimeoutMillis = 10_000;

    /**
     * 完整调用的超时时间，单位为毫秒；零表示不额外限制。
     */
    private int callTimeoutMillis;

    /**
     * 连接池允许保留的最大空闲连接数。
     */
    private int maxIdleConnections = 32;

    /**
     * 空闲连接保活时间，单位为毫秒。
     */
    private long keepAliveDurationMillis = 300_000L;

    /**
     * Dispatcher 允许的最大并发请求数。
     */
    private int maxRequests = 128;

    /**
     * 同一主机允许的最大并发请求数。
     */
    private int maxRequestsPerHost = 128;

    /**
     * 流式消费线程池核心线程数；保留用于统一配置语义。
     */
    private int streamCorePoolSize = 64;

    /**
     * 流式消费线程池最大线程数；保留用于统一配置语义。
     */
    private int streamMaxPoolSize = 64;

    /**
     * 流式消费任务队列容量；保留用于统一配置语义。
     */
    private int streamQueueCapacity = 128;

    /**
     * 流式消费线程空闲保活时间，单位为毫秒。
     */
    private long streamKeepAliveMillis = 60_000L;

    /**
     * 显式队列订阅允许缓存的最大事件数。
     */
    private int streamEventQueueCapacity = 1_024;

    /**
     * SSE 连接异常关闭后的最大重连次数。
     */
    private int streamReconnectMaxAttempts = 5;

    /**
     * SSE 首次重连基础延迟，单位为毫秒。
     */
    private long streamReconnectInitialDelayMillis = 1_000L;

    /**
     * SSE 指数退避的最大延迟，单位为毫秒。
     */
    private long streamReconnectMaxDelayMillis = 10_000L;

    /**
     * OkHttp 是否自动恢复可重试的连接故障。
     */
    private boolean retryOnConnectionFailure = true;

    /**
     * 是否校验 HTTPS 主机名和证书。
     */
    private boolean verifySsl = true;

    /**
     * 未显式指定时使用的默认模型。
     */
    private String defaultModel = HermesApiConstants.DEFAULT_MODEL;

    /**
     * 未显式指定时使用的默认系统指令。
     */
    private String defaultInstructions;

    /**
     * 未显式指定时使用的默认模型提供方。
     */
    private String defaultProvider;

    /**
     * <p>解析用于 Bearer 鉴权的 API key。</p>
     *
     * @return 配置的 API key；未配置时返回空字符串
     * @since 1.0.0
     */
    public String resolveApiKey() {
        return Objects.toString(apiKey, "");
    }
}
