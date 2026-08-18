package io.github.easy4j.hermes.cli;

import io.github.easy4j.hermes.HermesClientConfig;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 本地 {@code hermes} CLI 子进程执行器。
 */
public class HermesCliExecutor {

    private static final Logger log = LoggerFactory.getLogger(HermesCliExecutor.class);

    private final HermesClientConfig config;

    public HermesCliExecutor(HermesClientConfig config) {
        this.config = config;
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     */
    public HermesCliResult execute(String... args) {
        CommandLine cmd = CommandLine.parse(config.getLocalExecutable());
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
