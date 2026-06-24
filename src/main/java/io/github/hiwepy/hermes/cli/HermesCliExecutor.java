package io.github.hiwepy.hermes.cli;

import io.github.hiwepy.hermes.HermesCliConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 本地 {@code hermes} CLI 子进程执行器。
 */
@Slf4j
public class HermesCliExecutor {

    private final HermesCliConfig config;

    /**
     * @param config CLI 配置，不得为 null
     */
    public HermesCliExecutor(HermesCliConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * 同步执行 CLI 命令，返回执行结果。
     */
    public HermesCliResult execute(String... args) {
        CommandLine cmd = new CommandLine(config.getExecutable());
        for (String arg : args) {
            cmd.addArgument(arg);
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        executor.setStreamHandler(new org.apache.commons.exec.PumpStreamHandler(stdout, stderr));

        File workingDirectory = resolveWorkingDirectory();
        if (workingDirectory != null) {
            executor.setWorkingDirectory(workingDirectory);
        }

        long timeoutMs = config.getTimeout() * 1000L;
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
            HermesCliConfig probeConfig = copyForProbe(config);
            HermesCliResult result = new HermesCliExecutor(probeConfig).execute("--version");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    private static HermesCliConfig copyForProbe(HermesCliConfig source) {
        HermesCliConfig copy = new HermesCliConfig();
        copy.setExecutable(source.getExecutable());
        copy.setWorkingDirectory(source.getWorkingDirectory());
        copy.setMaxConcurrentExecutions(source.getMaxConcurrentExecutions());
        int probeSec = source.getProbeTimeoutSeconds();
        if (probeSec <= 0) {
            probeSec = 5;
        }
        copy.setTimeout(probeSec);
        copy.setProbeTimeoutSeconds(probeSec);
        return copy;
    }

    private File resolveWorkingDirectory() {
        String dir = config.getWorkingDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            return null;
        }
        return new File(dir.trim());
    }
}
