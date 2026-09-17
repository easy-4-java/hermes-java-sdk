package io.github.easy4j.hermes.cli;

import io.github.easy4j.hermes.HermesCliConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 执行器加固回归测试：argv 原样传递、真实退出码、UTF-8 解码、stdin 关闭与超时判定。
 *
 * @since 1.0.0
 */
class HermesCliExecutorHardeningTest {

    /** 参数回显替身脚本绝对路径（surefire 工作目录为模块根目录）。 */
    private static final String ECHO =
            Paths.get("src", "test", "resources", "hermes-echo.sh").toAbsolutePath().toString();

    private HermesCliConfig configFor(String executable) {
        HermesCliConfig config = new HermesCliConfig();
        config.setExecutable(executable);
        config.setTimeout(2);
        return config;
    }

    @Test
    void shouldPassArgumentsRawWithoutEmbeddedQuotes() {
        HermesCliExecutor executor = new HermesCliExecutor(configFor(ECHO));

        HermesCliResult result = executor.execute("--model", "kimi k2", "--prompt", "写个测试");

        assertEquals("--model kimi k2 --prompt 写个测试", result.getStdout(),
                "含空格参数必须原样进入 argv，不得嵌入字面双引号");
    }

    @Test
    void shouldPreserveRealExitCodeAndStreamsOnNonZeroExit() {
        HermesCliExecutor executor = new HermesCliExecutor(configFor("/bin/sh"));

        HermesCliResult result = executor.execute("-c", "echo out-marker; echo err-marker 1>&2; exit 7");

        assertEquals(7, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.getStdout().contains("out-marker"), "非零退出时 stdout 必须保留");
        assertTrue(result.getStderr().contains("err-marker"), "非零退出时 stderr 必须保留");
    }

    @Test
    void shouldDecodeUtf8OutputRegardlessOfPlatformCharset() {
        // POSIX printf 八进制转义输出 你好 的原始 UTF-8 字节；平台默认字符集解码
        // 在 C locale 下会损坏。注意 Java 字面量的反斜杠必须双写，否则八进制转义
        // 会在编译期被消费。
        HermesCliExecutor executor = new HermesCliExecutor(configFor("/bin/sh"));

        HermesCliResult result = executor.execute("-c", "printf '\\344\\275\\240\\345\\245\\275'");

        assertEquals("你好", result.getStdout());
    }

    @Test
    void shouldFeedStdinThenClosePipe() {
        // 无参 cat 回显 stdin：验证载荷写入与管道关闭。
        HermesCliExecutor executor = new HermesCliExecutor(configFor("/bin/cat"));

        HermesCliResult result = executor.executeWithStdin("payload-text");

        assertEquals(0, result.getExitCode());
        assertEquals("payload-text", result.getStdout());
    }

    @Test
    void shouldExecuteWithoutStdinAsBefore() {
        HermesCliExecutor executor = new HermesCliExecutor(configFor(ECHO));

        assertEquals("plain", executor.executeWithStdin(null, "plain").getStdout());
        assertEquals("plain", executor.executeWithStdin("", "plain").getStdout());
    }

    @Test
    void shouldTimeoutOnHangingProcess() {
        HermesCliConfig config = configFor("/bin/sh");
        config.setTimeout(1);
        HermesCliExecutor executor = new HermesCliExecutor(config);

        HermesCliResult result = executor.execute("-c", "sleep 30");

        assertEquals(-1, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.isTimeout(), "stderr 必须携带超时说明");
    }

    @Test
    void shouldDelegateNewDocumentedCommands() {
        HermesCli cli = new HermesCli(new HermesCliExecutor(configFor(ECHO)));

        assertTrue(cli.acp().getStdout().contains("acp"));
        assertTrue(cli.serve("--port", "8080").getStdout().contains("serve --port 8080"));
        assertTrue(cli.desktop().getStdout().contains("desktop"));
        assertTrue(cli.skillsBrowse().getStdout().contains("skills browse"));
        assertTrue(cli.skillsOptIn("k8s").getStdout().contains("skills opt-in k8s"));
        assertTrue(cli.egressSetup().getStdout().contains("egress setup"));
        assertTrue(cli.egressStart().getStdout().contains("egress start"));
        assertTrue(cli.importData("backup.zip").getStdout().contains("import backup.zip"));
        assertTrue(cli.update("--check").getStdout().contains("update --check"));
    }

    @Test
    void shouldIgnoreNullArguments() {
        HermesCliExecutor executor = new HermesCliExecutor(configFor(ECHO));

        HermesCliResult result = executor.execute("hello", null, "world");

        assertEquals(0, result.getExitCode());
        assertEquals("hello world", result.getStdout());
    }
}
