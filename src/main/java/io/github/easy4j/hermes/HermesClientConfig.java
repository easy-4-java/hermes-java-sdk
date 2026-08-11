package io.github.easy4j.hermes;
import lombok.Data;

/**
 * <p>Hermes 客户端组合配置。</p>
 *
 * <p>聚合 HTTP 与本地 CLI 配置，可作为纯 POJO 使用，也可映射到外部配置系统。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class HermesClientConfig {

    /** 客户端所有通信通道共享的调试配置。 */
    private final HermesDebugConfig debug = new HermesDebugConfig();

    /**
     * HTTP 与 SSE 通道配置。
     */
    private final HermesHttpClientConfig http = new HermesHttpClientConfig(debug);

    /**
     * 本地 Hermes CLI 客户端。
     */
    private final HermesCliConfig cli = new HermesCliConfig(debug);
}
