package io.github.hiwepy.hermes.cli.availability;

/**
 * Hermes CLI 可用性探测结论分类。
 */
public enum HermesCliAvailabilityStatus {

    /** {@code hermes --version} 探测成功。 */
    AVAILABLE,

    /** 未配置可执行文件。 */
    EXECUTABLE_NOT_CONFIGURED,

    /** 路径不存在或 PATH 中找不到。 */
    EXECUTABLE_NOT_FOUND,

    /** 存在但不可执行。 */
    EXECUTABLE_NOT_EXECUTABLE,

    /** 进程无法启动。 */
    SPAWN_FAILED,

    /** 非零退出。 */
    NON_ZERO_EXIT,

    /** 探测超时。 */
    TIMEOUT,

    /** 其它失败。 */
    FAILED
}
