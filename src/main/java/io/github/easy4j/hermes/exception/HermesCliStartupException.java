package io.github.easy4j.hermes.exception;

import io.github.easy4j.hermes.cli.availability.HermesCliAvailabilityReport;
import lombok.Getter;

/**
 * 应用启动阶段 Hermes CLI 不可用且配置为 fail-fast 时抛出。
 */
@Getter
public class HermesCliStartupException extends RuntimeException {

    private final HermesCliAvailabilityReport availabilityReport;

    /**
     * @param message 诊断说明
     * @param report  探测报告
     */
    public HermesCliStartupException(String message, HermesCliAvailabilityReport report) {
        super(message);
        this.availabilityReport = report;
    }
}
