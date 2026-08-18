package io.github.easy4j.hermes;

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

<<<<<<< HEAD
    /** HTTP/API Server 相关配置 */
    private final HermesHttpClientConfig http = new HermesHttpClientConfig();
=======
    // ============================================================
    // HTTP 相关配置
    // ============================================================

    /**
     * 是否启用 HTTP API 功能。
     * <p>为 false 时跳过 HTTP 客户端初始化和检查。</p>
     */
    private boolean httpEnabled = true;

    /**
     * 启动时是否探测 HTTP 服务可用性。
     */
    private boolean httpStartupCheckEnabled = true;

    /**
     * HTTP 服务不可用时是否快速失败。
     */
    private boolean httpFailFastOnUnavailable = false;

    // ============================================================
    // CLI 相关配置
    // ============================================================

    /**
     * 是否启用本地 CLI 功能。
     * <p>为 false 时跳过 CLI 相关初始化和检查。</p>
     */
    private boolean cliEnabled = true;

    /**
     * 启动时是否探测 CLI 可用性。
     */
    private boolean cliStartupCheckEnabled = true;

    /**
     * CLI 不可用时是否快速失败。
     */
    private boolean cliFailFastOnUnavailable = false;

    // ============================================================
    // Server & Auth
    // ============================================================

    /**
     * Hermes API Server 根地址，例如 {@code http://localhost:8642}。
     */
    private String serverUrl = HermesApiConstants.DEFAULT_SERVER_URL;
>>>>>>> ec61105 (feat: add startup check with httpEnabled/httpStartupCheckEnabled/httpFailFastOnUnavailable and cliEnabled/cliStartupCheckEnabled/cliFailFastOnUnavailable config)

    /** 本地 CLI 相关配置 */
    private final HermesCliConfig cli = new HermesCliConfig();
}
