package io.github.easy4j.hermes;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import lombok.Data;

/**
 * Hermes 客户端统一配置（纯 POJO，可与 Spring {@code @ConfigurationProperties} 映射）。
 * <p>
 * 组合 {@link HermesHttpClientConfig}（HTTP/API Server 相关）与 {@link HermesCliConfig}（本地 CLI 相关），
 * 作为 {@link HermesClient} 等统一入口的配置载体。
 * </p>
 */
@Data
public class HermesClientConfig {

    /** HTTP/API Server 相关配置 */
    private final HermesHttpClientConfig http = new HermesHttpClientConfig();

    /** 本地 CLI 相关配置 */
    private final HermesCliConfig cli = new HermesCliConfig();
}
