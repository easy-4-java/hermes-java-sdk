package io.github.easy4j.hermes.cli;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

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
