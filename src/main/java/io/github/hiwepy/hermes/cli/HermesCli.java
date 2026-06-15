package io.github.hiwepy.hermes.cli;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 本地 {@code hermes} CLI 命令封装，覆盖官方 CLI 文档中的主要子命令。
 */
@Slf4j
public class HermesCli {

    private final HermesCliExecutor executor;

    public HermesCli(HermesCliExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public HermesCliExecutor executor() { return executor; }

    // ============================================================
    // Version & Help
    // ============================================================

    public HermesCliResult version() { return executor.execute("--version"); }
    public HermesCliResult help() { return executor.execute("--help"); }

    // ============================================================
    // Chat
    // ============================================================

    /** {@code hermes chat} — 交互式聊天 */
    /** {@code hermes chat --toolsets "web,terminal"} — 指定工具集 */
    public HermesCliResult chatToolsets(String toolsets) { return executor.execute("chat", "--toolsets", toolsets); }
    public HermesCliResult chat(String... extraArgs) {
        return executor.execute(prefix("chat", extraArgs));
    }

    /** {@code hermes -z "<query>"} — one-shot 模式 */
    public HermesCliResult chatOneShot(String query) { return executor.execute("-z", query); }

    /** {@code hermes -z "<query>" --model <model>} */
    public HermesCliResult chatOneShot(String query, String model) {
        return executor.execute("-z", query, "--model", model);
    }

    /** {@code hermes -z "<query>" --model <model> --provider <provider>} */
    public HermesCliResult chatOneShot(String query, String model, String provider) {
        return executor.execute("-z", query, "--model", model, "--provider", provider);
    }


    /** {@code hermes chat -q "<query>"} — chat 子命令模式 */
    public HermesCliResult chatQuery(String query) { return executor.execute("chat", "-q", query); }
    public HermesCliResult chatQuery(String query, String model) {
        return executor.execute("chat", "-q", query, "--model", model);
    }
    public HermesCliResult chatQuery(String query, String model, String provider) {
        return executor.execute("chat", "-q", query, "--model", model, "--provider", provider);
    }

    /** {@code hermes -w -z "<query>"} — worktree 单查询模式 */
    public HermesCliResult worktreeOneShot(String query) { return executor.execute("-w", "-z", query); }
    /** {@code hermes -w} — worktree 交互模式 */
    public HermesCliResult worktree() { return executor.execute("-w"); }

    /** {@code hermes --continue} / {@code -c} — 恢复最近的 CLI 会话 */
    public HermesCliResult continueSession() { return executor.execute("-c"); }
    /** {@code hermes --resume <id>} — 恢复指定会话 */
    public HermesCliResult resumeSession(String id) { return executor.execute("--resume", id); }

    /** {@code hermes --tui} — TUI 模式 */

    // ============================================================
    // Model & Provider
    // ============================================================

    public HermesCliResult model() { return executor.execute("model"); }
    public HermesCliResult model(String modelName) { return executor.execute("model", modelName); }
    public HermesCliResult fallback() { return executor.execute("fallback"); }

    // ============================================================
    // Setup & Config
    // ============================================================

    public HermesCliResult setup() { return executor.execute("setup"); }
    public HermesCliResult setupPortal() { return executor.execute("setup", "--portal"); }

    public HermesCliResult configShow() { return executor.execute("config", "show"); }
    public HermesCliResult configEdit() { return executor.execute("config", "edit"); }
    public HermesCliResult configSet(String key, String value) { return executor.execute("config", "set", key, value); }
    public HermesCliResult configPath() { return executor.execute("config", "path"); }
    public HermesCliResult configEnvPath() { return executor.execute("config", "env-path"); }
    public HermesCliResult configCheck() { return executor.execute("config", "check"); }
    public HermesCliResult configMigrate() { return executor.execute("config", "migrate"); }

    public HermesCliResult profile() { return executor.execute("profile"); }

    // ============================================================
    // Gateway & Daemon
    // ============================================================

    public HermesCliResult gateway() { return executor.execute("gateway"); }
    public HermesCliResult gatewayRun() { return executor.execute("gateway", "run"); }
    public HermesCliResult gatewayStart() { return executor.execute("gateway", "start"); }
    public HermesCliResult gatewayStop() { return executor.execute("gateway", "stop"); }
    public HermesCliResult gatewayStatus() { return executor.execute("gateway", "status"); }
    public HermesCliResult gatewayInstall() { return executor.execute("gateway", "install"); }
    public HermesCliResult gatewayInstallSystem() { return executor.execute("gateway", "install", "--system"); }

    // ============================================================
    // Diagnostics
    // ============================================================

    public HermesCliResult doctor() { return executor.execute("doctor"); }
    public HermesCliResult doctorFix() { return executor.execute("doctor", "--fix"); }
    public HermesCliResult status() { return executor.execute("status"); }
    public HermesCliResult statusDeep() { return executor.execute("status", "--deep"); }
    public HermesCliResult dump() { return executor.execute("dump"); }
    public HermesCliResult logs() { return executor.execute("logs"); }
    public HermesCliResult logs(int lines) { return executor.execute("logs", "-n", String.valueOf(lines)); }
    public HermesCliResult logsFollow() { return executor.execute("logs", "-f"); }
    public HermesCliResult health() { return executor.execute("health"); }

    // ============================================================
    // Sessions
    // ============================================================

    public HermesCliResult sessions() { return executor.execute("sessions"); }
    public HermesCliResult sessionsList() { return executor.execute("sessions", "list"); }
    public HermesCliResult sessionsShow(String id) { return executor.execute("sessions", "show", id); }
    public HermesCliResult sessionsDelete(String id) { return executor.execute("sessions", "delete", id); }
    public HermesCliResult sessionsFork(String id) { return executor.execute("sessions", "fork", id); }
    /** {@code hermes fork <id>} — 分支会话 */
    public HermesCliResult fork(String id) { return executor.execute("fork", id); }
    public HermesCliResult sessionsRename(String id, String title) { return executor.execute("sessions", "rename", id, title); }

    // ============================================================
    // Skills, Tools, Memory
    // ============================================================

    public HermesCliResult skills() { return executor.execute("skills"); }
    public HermesCliResult skillsList() { return executor.execute("skills", "list"); }
    public HermesCliResult skillsSearch(String query) { return executor.execute("skills", "search", query); }
    public HermesCliResult skillsInstall(String slug) { return executor.execute("skills", "install", slug); }
    public HermesCliResult skillsRemove(String slug) { return executor.execute("skills", "remove", slug); }

    public HermesCliResult tools() { return executor.execute("tools"); }

    public HermesCliResult memory() { return executor.execute("memory"); }

    // ============================================================
    // Cron
    // ============================================================

    public HermesCliResult cron() { return executor.execute("cron"); }
    public HermesCliResult cronList() { return executor.execute("cron", "list"); }
    public HermesCliResult cronAdd(String schedule, String prompt) {
        return executor.execute("cron", "add", schedule, prompt);
    }
    public HermesCliResult cronRemove(String id) { return executor.execute("cron", "remove", id); }

    // ============================================================
    // MCP & LSP
    // ============================================================

    public HermesCliResult mcp() { return executor.execute("mcp"); }
    public HermesCliResult lsp() { return executor.execute("lsp"); }

    // ============================================================
    // Security & Hooks
    // ============================================================

    public HermesCliResult security() { return executor.execute("security"); }
    public HermesCliResult hooks() { return executor.execute("hooks"); }
    public HermesCliResult secrets() { return executor.execute("secrets"); }

    // ============================================================
    // Backup & Update
    // ============================================================

    public HermesCliResult backup() { return executor.execute("backup"); }
    public HermesCliResult update() { return executor.execute("update"); }
    public HermesCliResult uninstall() { return executor.execute("uninstall"); }

    // ============================================================
    // Pairing & Login
    // ============================================================

    public HermesCliResult pairing() { return executor.execute("pairing"); }
    public HermesCliResult login() { return executor.execute("login"); }
    public HermesCliResult logout() { return executor.execute("logout"); }

    // ============================================================
    // Dashboard & UI
    // ============================================================

    public HermesCliResult dashboard() { return executor.execute("dashboard"); }
    public HermesCliResult tui() { return executor.execute("tui"); }
    public HermesCliResult completion() { return executor.execute("completion"); }

    // ============================================================
    // Plugins, Bundles, Curator
    // ============================================================

    public HermesCliResult plugins() { return executor.execute("plugins"); }
    public HermesCliResult bundles() { return executor.execute("bundles"); }
    public HermesCliResult curator() { return executor.execute("curator"); }

    // ============================================================
    // Internal
    // ============================================================

    private static String[] prefix(String first, String... rest) {
        String[] result = new String[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }
}
