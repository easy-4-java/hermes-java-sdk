package io.github.easy4j.hermes.cli.availability;
/**
 * @author <a href="https://github.com/loong10k">@Loong Wan</a>
 */

import io.github.easy4j.hermes.HermesCliConfig;
import io.github.easy4j.hermes.cli.HermesCli;
import io.github.easy4j.hermes.cli.HermesCliExecutor;
import io.github.easy4j.hermes.cli.HermesCliResult;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * 探测本机 {@code hermes} 是否已安装且可执行 {@code hermes --version}。
 */
public class HermesCliAvailabilityChecker {

    /**
     * 使用与运行时一致的配置探测 CLI。
     *
     * @param config CLI 配置，不得为 null
     * @return 探测报告
     */
    public HermesCliAvailabilityReport check(HermesCliConfig config) {
        Objects.requireNonNull(config, "config");
        String configured = config.getExecutable();
        if (isBlank(configured)) {
            return unavailable(
                    HermesCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED,
                    configured,
                    null,
                    "hermes.cli.executable is blank",
                    null);
        }
        String trimmed = configured.trim();
        Optional<String> resolved = resolveExecutablePath(trimmed);
        if (!resolved.isPresent()) {
            if (looksLikePath(trimmed)) {
                File file = new File(trimmed);
                if (!file.exists()) {
                    return unavailable(
                            HermesCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                            trimmed,
                            null,
                            "executable file does not exist: " + file.getAbsolutePath(),
                            null);
                }
                return unavailable(
                        HermesCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE,
                        trimmed,
                        file.getAbsolutePath(),
                        "executable exists but is not executable: " + file.getAbsolutePath(),
                        null);
            }
            return unavailable(
                    HermesCliAvailabilityStatus.EXECUTABLE_NOT_FOUND,
                    trimmed,
                    null,
                    "executable not found on PATH: " + trimmed,
                    null);
        }

        HermesCliConfig probeConfig = copyForProbe(config);
        HermesCliExecutor probeExecutor = new HermesCliExecutor(probeConfig);
        HermesCliResult result = new HermesCli(probeExecutor).version();
        if (result.isSuccess()) {
            return HermesCliAvailabilityReport.builder()
                    .status(HermesCliAvailabilityStatus.AVAILABLE)
                    .available(true)
                    .configuredExecutable(trimmed)
                    .resolvedExecutablePath(resolved.get())
                    .message("hermes --version succeeded")
                    .probeResult(result)
                    .build();
        }
        if (result.getExitCode() == -1 && containsTimeoutHint(result)) {
            return unavailable(
                    HermesCliAvailabilityStatus.TIMEOUT,
                    trimmed,
                    resolved.get(),
                    "hermes --version timed out",
                    result);
        }
        if (result.getExitCode() == -1 && isSpawnFailure(result)) {
            return unavailable(
                    HermesCliAvailabilityStatus.SPAWN_FAILED,
                    trimmed,
                    resolved.get(),
                    firstLine(result.getStderr()),
                    result);
        }
        return unavailable(
                HermesCliAvailabilityStatus.NON_ZERO_EXIT,
                trimmed,
                resolved.get(),
                "hermes --version exitCode=" + result.getExitCode(),
                result);
    }

    /**
     * 解析可执行文件：绝对/相对路径直接检查；否则在 {@code PATH} 中查找。
     */
    static Optional<String> resolveExecutablePath(String executable) {
        if (isBlank(executable)) {
            return Optional.empty();
        }
        String trimmed = executable.trim();
        File direct = new File(trimmed);
        if (looksLikePath(trimmed)) {
            if (direct.isFile() && direct.canExecute()) {
                return Optional.of(direct.getAbsolutePath());
            }
            return Optional.empty();
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return Optional.empty();
        }
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (isBlank(dir)) {
                continue;
            }
            File candidate = new File(dir.trim(), trimmed);
            if (candidate.isFile() && candidate.canExecute()) {
                return Optional.of(candidate.getAbsolutePath());
            }
        }
        return Optional.empty();
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

    private static boolean looksLikePath(String executable) {
        return executable.contains("/") || executable.contains("\\") || new File(executable).isAbsolute();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean containsTimeoutHint(HermesCliResult result) {
        String combined = result.getStderr() + result.getStdout();
        return combined.toLowerCase().contains("timed out");
    }

    private static boolean isSpawnFailure(HermesCliResult result) {
        String stderr = result.getStderr();
        return stderr != null
                && (stderr.contains("could not be started")
                || stderr.contains("No such file")
                || stderr.contains("spawn"));
    }

    private static String firstLine(String text) {
        if (isBlank(text)) {
            return "hermes execution failed";
        }
        int idx = text.indexOf('\n');
        return idx >= 0 ? text.substring(0, idx) : text;
    }

    private static HermesCliAvailabilityReport unavailable(
            HermesCliAvailabilityStatus status,
            String configured,
            String resolved,
            String message,
            HermesCliResult partial) {
        return HermesCliAvailabilityReport.builder()
                .status(status)
                .available(false)
                .configuredExecutable(configured)
                .resolvedExecutablePath(resolved)
                .message(message)
                .probeResult(partial)
                .build();
    }
}
