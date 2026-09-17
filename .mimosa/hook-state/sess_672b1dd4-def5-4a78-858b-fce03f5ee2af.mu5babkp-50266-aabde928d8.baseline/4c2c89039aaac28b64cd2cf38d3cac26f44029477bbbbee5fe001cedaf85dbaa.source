package io.github.easy4j.hermes.cli.availability;
/**
 * <p>Hermes CLI 可用性状态枚举。</p>
 *
 * <p>区分可用、未配置、文件不存在、不可执行、超时和执行失败等探测结论。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public enum HermesCliAvailabilityStatus {

    /**
     * CLI 已配置、可执行且版本探测成功。
     */
    AVAILABLE,

    /**
     * CLI 可执行文件名称或路径未配置。
     */
    EXECUTABLE_NOT_CONFIGURED,

    /**
     * 配置的 CLI 可执行文件无法解析或不存在。
     */
    EXECUTABLE_NOT_FOUND,

    /**
     * CLI 文件存在但当前进程没有执行权限。
     */
    EXECUTABLE_NOT_EXECUTABLE,

    /**
     * 操作系统无法创建 CLI 子进程。
     */
    SPAWN_FAILED,

    /**
     * CLI 版本探测以非零退出码结束。
     */
    NON_ZERO_EXIT,

    /**
     * CLI 版本探测超过配置时限。
     */
    TIMEOUT,

    /**
     * CLI 探测发生未归类异常。
     */
    FAILED
}
