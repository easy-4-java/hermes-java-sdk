package io.github.hiwepy.hermes;

import io.github.hiwepy.hermes.api.HermesApiConstants;
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
