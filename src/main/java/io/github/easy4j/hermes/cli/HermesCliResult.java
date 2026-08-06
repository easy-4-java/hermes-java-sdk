package io.github.easy4j.hermes.cli;

import lombok.Data;

/**
 * CLI 执行结果。
 */
@Data
public class HermesCliResult {

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
