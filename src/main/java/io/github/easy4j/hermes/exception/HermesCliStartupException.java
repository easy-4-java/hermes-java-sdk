package io.github.easy4j.hermes.exception;
import io.github.easy4j.hermes.cli.availability.HermesCliAvailabilityReport;
import lombok.Getter;

/**
 * <p>Hermes CLI 启动检查异常。</p>
 *
 * <p>当 CLI 不可用且启用快速失败时携带可用性报告终止初始化。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Getter
public class HermesCliStartupException extends RuntimeException {

    /**
     * CLI 启动检查报告。
     */
    private final HermesCliAvailabilityReport availabilityReport;

    /**
     * <p>创建 HermesCliStartupException 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param message 异常或诊断消息
     * @param report CLI 可用性探测报告
     * @since 1.0.0
     */
    public HermesCliStartupException(String message, HermesCliAvailabilityReport report) {
        super(message);
        this.availabilityReport = report;
    }
}
