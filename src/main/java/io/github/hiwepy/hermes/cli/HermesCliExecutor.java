package io.github.hiwepy.hermes.cli;

import io.github.hiwepy.hermes.HermesClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * 本地 {@code hermes} CLI 子进程执行器。
 */
@Slf4j
public class HermesCliExecutor {

    private final HermesClientConfig config;

    public HermesCliExecutor(HermesClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     */
    public HermesCliResult execute(String... args) {
        CommandLine cmd = new CommandLine(config.getLocalExecutable());
        for (String arg : args) {
            cmd.addArgument(arg);
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        executor.setStreamHandler(new org.apache.commons.exec.PumpStreamHandler(stdout, stderr));

        long timeoutMs = config.getLocalTimeoutSeconds() * 1000L;
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        try {
            int exitCode = executor.execute(cmd);
            String out = stdout.toString().trim();
            String err = stderr.toString().trim();
            log.debug("hermes CLI executed: exitCode={}, stdout={}, stderr={}", exitCode, out, err);
            return new HermesCliResult(exitCode, out, err);
        } catch (IOException e) {
            log.warn("CLI execution failed", e);
            return new HermesCliResult(-1, "", e.getMessage());
        }
    }

    /**
     * 探测 CLI 是否可用（执行 {@code hermes --version}）。
     */
    public boolean probe() {
        try {
            HermesCliResult result = execute("--version");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
}
