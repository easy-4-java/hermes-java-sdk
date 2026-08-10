package io.github.easy4j.hermes.cli;
import io.github.easy4j.hermes.HermesCliConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * <p>Hermes CLI 子进程执行器。</p>
 *
 * <p>负责命令构造、工作目录、超时等待以及标准输出和错误输出收集；调用方负责限制并发子进程数。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesCliExecutor {

    /**
     * 当前客户端使用的配置快照。
     */
    private final HermesCliConfig config;

    /**
     * <p>创建 HermesCliExecutor 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param config 客户端配置，不得为 {@code null}
     * @since 1.0.0
     */
    public HermesCliExecutor(HermesCliConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * <p>执行 Hermes CLI 命令并等待结果。</p>
     *
     * @param args 传递给 Hermes CLI 的参数
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult execute(String... args) {
        // 按参数边界构造命令，避免手工拼接引入空格转义错误和命令注入风险。
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
        // Watchdog 负责超时终止子进程；此等待只发生在显式 CLI 调用，不占用 HTTP/SSE 线程。
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
     * <p>探测 Hermes CLI 是否可执行。</p>
     *
     * @return CLI 版本命令成功退出时返回 {@code true}
     * @since 1.0.0
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
