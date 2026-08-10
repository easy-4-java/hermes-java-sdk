package io.github.easy4j.hermes.cli.availability;
import io.github.easy4j.hermes.cli.HermesCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * <p>Hermes CLI 可用性报告。</p>
 *
 * <p>记录探测状态、原因、可执行文件、版本、耗时和底层异常。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Getter
@Builder
public class HermesCliAvailabilityReport {

    /**
     * 服务端状态。
     */
    private final HermesCliAvailabilityStatus status;
    /**
     * CLI 是否可以成功执行版本探测。
     */
    private final boolean available;
    /**
     * 配置中的 CLI 可执行文件名称或路径。
     */
    private final String configuredExecutable;
    /**
     * 解析后的 CLI 可执行文件绝对路径。
     */
    private final String resolvedExecutablePath;
    /**
     * 可用性报告或异常消息。
     */
    private final String message;
    /**
     * CLI 版本探测命令结果。
     */
    private final HermesCliResult probeResult;

    /**
     * <p>判断 CLI 是否通过可用性探测。</p>
     *
     * @return CLI 已配置且版本探测成功时返回 {@code true}
     * @since 1.0.0
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * <p>生成便于日志和异常展示的 CLI 可用性诊断文本。</p>
     *
     * @return 包含状态、路径、耗时和失败原因的诊断文本
     * @since 1.0.0
     */
    public String toDiagnosticMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hermes CLI ");
        sb.append(available ? "ready" : "unavailable");
        sb.append(" [").append(status).append(']');
        if (configuredExecutable != null) {
            sb.append(" executable=").append(configuredExecutable);
        }
        if (resolvedExecutablePath != null) {
            sb.append(" resolved=").append(resolvedExecutablePath);
        }
        if (message != null && !message.isEmpty()) {
            sb.append(" — ").append(message);
        }
        return sb.toString();
    }
}
