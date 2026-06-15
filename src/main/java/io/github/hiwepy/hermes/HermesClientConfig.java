package io.github.hiwepy.hermes;

import io.github.hiwepy.hermes.api.HermesApiConstants;
import lombok.Data;

import java.util.Objects;

/**
 * Hermes 客户端配置（纯 POJO，可与 Spring {@code @ConfigurationProperties} 映射）。
 * <p>
 * 通过 Hermes API Server 进行 REST API 交互，也可使用本地 CLI 子进程。
 * </p>
 */
@Data
public class HermesClientConfig {

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

    /**
     * 是否校验 HTTPS 证书；为 false 时关闭校验（仅建议开发环境）。
     */
    private boolean verifySsl = true;

    /**
     * 本地 CLI 可执行文件名或绝对路径。
     */
    private String localExecutable = HermesApiConstants.DEFAULT_EXECUTABLE;

    /**
     * 本地 CLI 命令超时（秒）。
     */
    private int localTimeoutSeconds = HermesApiConstants.DEFAULT_LOCAL_TIMEOUT_SECONDS;

    /**
     * 探测本地 CLI 是否可用的超时（秒）。
     */
    private int localProbeTimeoutSeconds = HermesApiConstants.DEFAULT_PROBE_TIMEOUT_SECONDS;

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
