package io.github.easy4j.hermes;

import io.github.easy4j.hermes.api.HermesApiConstants;
import lombok.Data;

/**
 * Hermes 本地 CLI 客户端配置。
 * <p>
 * 涵盖本地 {@code hermes} 可执行文件路径、超时、工作目录等 CLI 运行时设置。
 * </p>
 */
@Data
public class HermesCliConfig {

    /**
     * 是否启用本地 CLI 功能。
     * <p>为 false 时跳过 CLI 相关初始化和检查。</p>
     */
    private boolean enabled = true;

    /**
     * 启动时是否探测 CLI 可用性。
     */
    private boolean startupCheckEnabled = false;

    /**
     * CLI 不可用时是否快速失败。
     */
    private boolean failFastOnUnavailable = false;

    /**
     * 本地可执行文件名或绝对路径。
     */
    private String executable = HermesApiConstants.DEFAULT_EXECUTABLE;

    /**
     * 本地 CLI 命令超时（秒）。
     */
    private int timeout = HermesApiConstants.DEFAULT_LOCAL_TIMEOUT_SECONDS;

    /**
     * 探测本地运行时是否可用的超时（秒）。
     */
    private int probeTimeoutSeconds = HermesApiConstants.DEFAULT_PROBE_TIMEOUT_SECONDS;

    /**
     * 本地 CLI 子进程工作目录；为空时使用 JVM 当前目录。
     */
    private String workingDirectory;

    /**
     * 本机 CLI 子进程最大并发数；小于等于 0 时不额外限制。
     */
    private int maxConcurrentExecutions = 0;
}
