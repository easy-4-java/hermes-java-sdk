package io.github.easy4j.hermes;
import io.github.easy4j.hermes.api.HermesApiConstants;
import lombok.Data;

import java.util.Objects;

/**
 * <p>Hermes 本地 CLI 配置。</p>
 *
 * <p>定义可执行文件、工作目录、执行超时和启动探测；并发数字段仅作为上层调度的预留配置。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class HermesCliConfig {

    /** CLI 与 HTTP/SSE 通道共享的调试策略。 */
    private final HermesDebugConfig debug;

    /** 使用独立的默认调试策略创建配置。 */
    public HermesCliConfig() {
        this(new HermesDebugConfig());
    }

    /**
     * 使用指定调试策略创建配置。
     *
     * @param debug 客户端共享调试策略
     */
    public HermesCliConfig(HermesDebugConfig debug) {
        this.debug = Objects.requireNonNull(debug, "debug");
    }

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
     * Hermes CLI 可执行文件名称或路径。
     */
    private String executable = HermesApiConstants.DEFAULT_EXECUTABLE;

    /**
     * CLI 子进程最长执行时间，单位为秒。
     */
    private int timeout = HermesApiConstants.DEFAULT_LOCAL_TIMEOUT_SECONDS;

    /**
     * CLI 版本探测超时，单位为秒。
     */
    private int probeTimeoutSeconds = HermesApiConstants.DEFAULT_PROBE_TIMEOUT_SECONDS;

    /**
     * CLI 子进程工作目录；为空时继承当前进程目录。
     */
    private String workingDirectory;

    /**
     * CLI 并发上限的预留配置；非正数表示未设置，当前 SDK 执行器不会据此创建线程池或实施限流。
     */
    private int maxConcurrentExecutions = 0;
}
