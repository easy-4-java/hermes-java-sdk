package io.github.easy4j.hermes.cli;
import lombok.Data;

/**
 * <p>Hermes CLI 执行结果。</p>
 *
 * <p>保存退出码、标准输出和标准错误，供调用方判断命令结果并读取诊断信息。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
public class HermesCliResult {

    /**
     * CLI 子进程退出码。
     */
    private final int exitCode;
    /**
     * CLI 标准输出文本。
     */
    private final String stdout;
    /**
     * CLI 标准错误文本。
     */
    private final String stderr;

    /**
     * <p>判断 CLI 子进程是否以零退出码成功结束。</p>
     *
     * @return 退出码为零时返回 {@code true}
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
