package io.github.easy4j.hermes.cli;
import io.github.easy4j.hermes.HermesCliConfig;
import okhttp3.extension.logging.HttpLogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <p>Hermes CLI 子进程执行器。</p>
 *
 * <p>负责命令构造、工作目录、超时等待以及标准输出和错误输出收集；调用方负责限制并发子进程数。</p>
 *
 * <p>加固约定：</p>
 * <ul>
 *   <li>参数经 {@code addArgument(arg, false)} 原样进入 argv——子进程经 exec 启动而非 shell，
 *       默认引号策略会把含空格参数的字面双引号烤进 argv，损坏多词 prompt 与路径；</li>
 *   <li>子进程始终收到（可能为空的）立即关闭的 stdin——读取型命令读到 EOF 即结束，
 *       已关闭管道也不会与输入泵产生写后关闭竞态；</li>
 *   <li>输出按 UTF-8 显式解码——{@code toString()} 走平台默认字符集，C locale 或
 *       Windows GBK 环境会损坏中文输出；</li>
 *   <li>非零退出经 {@link ExecuteException} 单独捕获，保留真实退出码与两路输出；
 *       超时判定用截止时间法，规避 {@code watchdog.killedProcess()} 的观察竞态。</li>
 * </ul>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesCliExecutor {

    private static final String TIMEOUT_PREFIX = "hermes CLI timed out after ";

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
        return runProcess(null, args);
    }

    /**
     * <p>执行 Hermes CLI 命令并向子进程 stdin 管道写入 {@code stdin} 内容。</p>
     *
     * <p>供从标准输入读取载荷的命令形态使用；{@code stdin} 为 {@code null} 或空时
     * 行为与 {@link #execute(String...)} 一致（子进程收到立即关闭的空管道）。失败语义
     * 与变参重载相同。</p>
     *
     * @param stdin 写入子进程标准输入的可选文本
     * @param args  传递给 Hermes CLI 的参数
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult executeWithStdin(String stdin, String... args) {
        return runProcess(stdin, args);
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

    /**
     * <p>以 UTF-8 显式解码子进程输出。</p>
     *
     * <p>{@code toString(Charset)} 是 Java 10+ API，JDK 8 线经由
     * {@code toString("UTF-8")}；UTF-8 在所有 JVM 上保证存在，
     * catch 分支不可达，仅为满足受检异常。</p>
     *
     * @param buffer 待解码的输出缓冲
     * @return UTF-8 解码后的文本
     * @since 1.0.0
     */
    private static String decodeUtf8(ByteArrayOutputStream buffer) {
        try {
            return buffer.toString("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private HermesCliResult runProcess(String stdin, String... args) {
        // 按参数边界构造命令，避免手工拼接引入空格转义错误和命令注入风险。
        CommandLine cmd = new CommandLine(config.getExecutable());
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            // handleQuoting=false：子进程经 exec(argv) 启动而非 shell，默认引号策略
            // 会把含空格参数的字面双引号烤进 argv，损坏多词 prompt 与路径。
            cmd.addArgument(arg, false);
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        // 始终向子进程提供（可能为空的）立即关闭的 stdin：消费型命令读到 EOF 即结束，
        // 已关闭管道不会与输入泵产生写后关闭竞态。
        byte[] stdinBytes = stdin == null ? new byte[0] : stdin.getBytes(StandardCharsets.UTF_8);
        executor.setStreamHandler(new PumpStreamHandler(stdout, stderr,
                new ByteArrayInputStream(stdinBytes)));

        File workingDirectory = resolveWorkingDirectory();
        if (workingDirectory != null) {
            executor.setWorkingDirectory(workingDirectory);
        }

        long timeoutMs = config.getTimeout() * 1000L;
        // Watchdog 负责超时终止子进程；此等待只发生在显式 CLI 调用，不占用 HTTP/SSE 线程。
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        long startNanos = System.nanoTime();
        try {
            int exitCode = executor.execute(cmd);
            String out = decodeUtf8(stdout).trim();
            String err = decodeUtf8(stderr).trim();
            if (config.getDebug().allows(HttpLogLevel.BASIC)) {
                log.debug("Hermes CLI executed: exitCode={}, stdoutLength={}, stderrLength={}",
                        exitCode, out.length(), err.length());
            }
            if (config.getDebug().allows(HttpLogLevel.BODY)) {
                log.debug("Hermes CLI output: stdout={}, stderr={}", truncate(out), truncate(err));
            }
            if (watchdog.killedProcess()) {
                return timeoutResult(stdout, stderr, timeoutMs);
            }
            return new HermesCliResult(exitCode, out, err);
        } catch (ExecuteException e) {
            // commons-exec 对每次非零退出（以及 watchdog 击杀）都会抛出 ExecuteException；
            // 抛出前输出泵已 join，两路缓冲是完整的——原样保留真实退出码与输出，
            // 超时判定用截止时间法规避 killedProcess() 的观察竞态。
            String out = decodeUtf8(stdout).trim();
            String err = decodeUtf8(stderr).trim();
            boolean timedOut = watchdog.killedProcess()
                    || System.nanoTime() - startNanos >= timeoutMs * 1_000_000L;
            if (timedOut) {
                log.warn("Hermes CLI timed out after {} ms", timeoutMs);
                return timeoutResult(stdout, stderr, timeoutMs);
            }
            if (config.getDebug().allows(HttpLogLevel.BASIC)) {
                log.debug("Hermes CLI failed: exitCode={}, stdoutLength={}, stderrLength={}",
                        e.getExitValue(), out.length(), err.length());
            }
            return new HermesCliResult(e.getExitValue(), out, err);
        } catch (IOException e) {
            log.warn("CLI execution failed", e);
            return new HermesCliResult(-1, "", e.getMessage());
        }
    }

    private HermesCliResult timeoutResult(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr,
                                          long timeoutMs) {
        String out = decodeUtf8(stdout).trim();
        String err = TIMEOUT_PREFIX + timeoutMs + " ms\n" + decodeUtf8(stderr).trim();
        return new HermesCliResult(-1, out, err);
    }

    private static HermesCliConfig copyForProbe(HermesCliConfig source) {
        HermesCliConfig copy = new HermesCliConfig(source.getDebug());
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

    private String truncate(String content) {
        int maxLength = config.getDebug().resolveMaxContentLength();
        return content.length() <= maxLength ? content : content.substring(0, maxLength) + "...<truncated>";
    }

    private File resolveWorkingDirectory() {
        String dir = config.getWorkingDirectory();
        if (dir == null || dir.trim().isEmpty()) {
            return null;
        }
        return new File(dir.trim());
    }
}
