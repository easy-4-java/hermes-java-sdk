package io.github.easy4j.hermes;

import io.github.easy4j.hermes.cli.HermesCli;
import io.github.easy4j.hermes.cli.HermesCliExecutor;
import io.github.easy4j.hermes.cli.HermesCliResult;
import io.github.easy4j.hermes.cli.availability.HermesCliAvailabilityChecker;
import io.github.easy4j.hermes.cli.availability.HermesCliAvailabilityReport;
import io.github.easy4j.hermes.cli.availability.HermesCliAvailabilityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HermesCliCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldMapEveryCliFacadeMethodToExecutorArguments() {
        RecordingExecutor executor = new RecordingExecutor();
        HermesCli cli = new HermesCli(executor);
        assertSame(executor, cli.executor());

        List<HermesCliResult> results = List.of(
                cli.version(), cli.help(), cli.chatToolsets("web"), cli.chat("--verbose"),
                cli.chatOneShot("q"), cli.chatOneShot("q", "m"), cli.chatOneShot("q", "m", "p"),
                cli.chatQuery("q"), cli.chatQuery("q", "m"), cli.chatQuery("q", "m", "p"),
                cli.worktreeOneShot("q"), cli.worktree(), cli.continueSession(), cli.resumeSession("id"),
                cli.model(), cli.model("m"), cli.fallback(), cli.setup(), cli.setupPortal(),
                cli.configShow(), cli.configEdit(), cli.configSet("k", "v"), cli.configPath(),
                cli.configEnvPath(), cli.configCheck(), cli.configMigrate(), cli.profile(),
                cli.gateway(), cli.gatewayRun(), cli.gatewayStart(), cli.gatewayStop(), cli.gatewayStatus(),
                cli.gatewayInstall(), cli.gatewayInstallSystem(), cli.doctor(), cli.doctorFix(), cli.status(),
                cli.statusDeep(), cli.dump(), cli.logs(), cli.logs(20), cli.logsFollow(), cli.health(),
                cli.sessions(), cli.sessionsList(), cli.sessionsShow("id"), cli.sessionsDelete("id"),
                cli.sessionsFork("id"), cli.fork("id"), cli.sessionsRename("id", "title"),
                cli.skills(), cli.skillsList(), cli.skillsSearch("q"), cli.skillsInstall("slug"),
                cli.skillsRemove("slug"), cli.tools(), cli.memory(), cli.cron(), cli.cronList(),
                cli.cronAdd("* * * * *", "prompt"), cli.cronRemove("id"), cli.mcp(), cli.lsp(),
                cli.security(), cli.hooks(), cli.secrets(), cli.backup(), cli.update(), cli.uninstall(),
                cli.pairing(), cli.login(), cli.logout(), cli.dashboard(), cli.tui(), cli.completion(),
                cli.plugins(), cli.bundles(), cli.curator());

        assertTrue(results.stream().allMatch(HermesCliResult::isSuccess));
        assertEquals(results.size(), executor.commands.size());
        assertEquals(List.of("chat", "--verbose"), executor.commands.get(3));
    }

    @Test
    void shouldExecuteAndProbeLocalProcesses() {
        HermesCliConfig echo = new HermesCliConfig();
        echo.setExecutable("/bin/echo");
        echo.setWorkingDirectory(tempDir.toString());
        echo.setTimeout(2);
        echo.setProbeTimeoutSeconds(0);
        HermesCliExecutor executor = new HermesCliExecutor(echo);
        HermesCliResult result = executor.execute("hello", "world");
        assertTrue(result.isSuccess());
        assertEquals("hello world", result.getStdout());
        assertTrue(executor.probe());

        HermesCliConfig missing = new HermesCliConfig();
        missing.setExecutable(tempDir.resolve("missing").toString());
        assertFalse(new HermesCliExecutor(missing).execute("--version").isSuccess());
        assertFalse(new HermesCliExecutor(missing).probe());
    }

    @Test
    void shouldClassifyCliAvailability() throws Exception {
        HermesCliAvailabilityChecker checker = new HermesCliAvailabilityChecker();
        HermesCliConfig config = new HermesCliConfig();
        config.setExecutable(" ");
        assertStatus(checker.check(config), HermesCliAvailabilityStatus.EXECUTABLE_NOT_CONFIGURED);

        config.setExecutable(tempDir.resolve("missing").toString());
        assertStatus(checker.check(config), HermesCliAvailabilityStatus.EXECUTABLE_NOT_FOUND);

        Path nonExecutable = tempDir.resolve("not-executable");
        Files.writeString(nonExecutable, "#!/bin/sh\nexit 0\n");
        nonExecutable.toFile().setExecutable(false);
        config.setExecutable(nonExecutable.toString());
        assertStatus(checker.check(config), HermesCliAvailabilityStatus.EXECUTABLE_NOT_EXECUTABLE);

        Path available = script("available", "#!/bin/sh\necho hermes-test\nexit 0\n");
        config.setExecutable(available.toString());
        HermesCliAvailabilityReport availableReport = checker.check(config);
        assertStatus(availableReport, HermesCliAvailabilityStatus.AVAILABLE);
        assertTrue(availableReport.toDiagnosticMessage().contains("ready"));

        Path failed = script("failed", "#!/bin/sh\necho bad >&2\nexit 3\n");
        config.setExecutable(failed.toString());
        HermesCliAvailabilityReport failedReport = checker.check(config);
        assertStatus(failedReport, HermesCliAvailabilityStatus.NON_ZERO_EXIT);
        assertTrue(failedReport.toDiagnosticMessage().contains("unavailable"));

        config.setExecutable("definitely-not-a-hermes-command");
        assertStatus(checker.check(config), HermesCliAvailabilityStatus.EXECUTABLE_NOT_FOUND);
    }

    private Path script(String name, String content) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content);
        path.toFile().setExecutable(true);
        return path;
    }

    private void assertStatus(HermesCliAvailabilityReport report, HermesCliAvailabilityStatus status) {
        assertEquals(status, report.getStatus());
        assertEquals(status == HermesCliAvailabilityStatus.AVAILABLE, report.isAvailable());
    }

    private static final class RecordingExecutor extends HermesCliExecutor {
        private final List<List<String>> commands = new ArrayList<>();

        private RecordingExecutor() {
            super(new HermesCliConfig());
        }

        @Override
        public HermesCliResult execute(String... args) {
            commands.add(List.of(args));
            return new HermesCliResult(0, "ok", "");
        }
    }
}
