package io.github.hiwepy.hermes.cli.availability;

import io.github.hiwepy.hermes.cli.HermesCliResult;
import lombok.Builder;
import lombok.Getter;

/**
 * Hermes CLI 启动/就绪探测结果。
 */
@Getter
@Builder
public class HermesCliAvailabilityReport {

    private final HermesCliAvailabilityStatus status;
    private final boolean available;
    private final String configuredExecutable;
    private final String resolvedExecutablePath;
    private final String message;
    private final HermesCliResult probeResult;

    /**
     * @return 是否可安全调用本地 {@code hermes}
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 构造面向日志/异常的诊断文本。
     *
     * @return 说明字符串
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
