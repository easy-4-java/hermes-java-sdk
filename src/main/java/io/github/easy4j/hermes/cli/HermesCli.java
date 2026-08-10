package io.github.easy4j.hermes.cli;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * <p>Hermes 本地 CLI 命令门面。</p>
 *
 * <p>将官方 CLI 的聊天、会话、网关、配置、诊断、技能和维护命令映射为类型化 Java 方法。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Slf4j
public class HermesCli {

    /**
     * 执行本地 Hermes 命令的子进程执行器。
     */
    private final HermesCliExecutor executor;

    /**
     * <p>创建 HermesCli 实例。</p>
     *
     * <p>依赖项和资源所有权由构造参数决定；必需参数为空时初始化失败。</p>
     *
     * @param executor 执行 Hermes 子进程的 CLI 执行器
     * @since 1.0.0
     */
    public HermesCli(HermesCliExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * <p>执行 Hermes CLI 的 {@code executor} 命令。</p>
     *
     * @return 当前 CLI 门面委托的子进程执行器
     * @since 1.0.0
     */
    public HermesCliExecutor executor() { return executor; }

    // ============================================================
    // Version & Help
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code version} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult version() { return executor.execute("--version"); }
    /**
     * <p>执行 Hermes CLI 的 {@code help} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult help() { return executor.execute("--help"); }

    // ============================================================
    // Chat
    // ============================================================

    /** {@code hermes chat} — 交互式聊天 */
    /**
     * <p>执行 Hermes CLI 的 {@code chatToolsets} 命令。</p>
     *
     * @param toolsets 逗号分隔的工具集名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatToolsets(String toolsets) { return executor.execute("chat", "--toolsets", toolsets); }
    /**
     * <p>返回聊天场景客户端。</p>
     *
     * @param extraArgs 附加 CLI 参数
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chat(String... extraArgs) {
        return executor.execute(prefix("chat", extraArgs));
    }

    /**
     * <p>执行 Hermes CLI 的 {@code chatOneShot} 命令。</p>
     *
     * @param query 查询或提示词
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatOneShot(String query) { return executor.execute("-z", query); }

    /**
     * <p>执行 Hermes CLI 的 {@code chatOneShot} 命令。</p>
     *
     * @param query 查询或提示词
     * @param model 模型名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatOneShot(String query, String model) {
        return executor.execute("-z", query, "--model", model);
    }

    /**
     * <p>执行 Hermes CLI 的 {@code chatOneShot} 命令。</p>
     *
     * @param query 查询或提示词
     * @param model 模型名称
     * @param provider 模型提供方名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatOneShot(String query, String model, String provider) {
        return executor.execute("-z", query, "--model", model, "--provider", provider);
    }


    /**
     * <p>执行 Hermes CLI 的 {@code chatQuery} 命令。</p>
     *
     * @param query 查询或提示词
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatQuery(String query) { return executor.execute("chat", "-q", query); }
    /**
     * <p>执行 Hermes CLI 的 {@code chatQuery} 命令。</p>
     *
     * @param query 查询或提示词
     * @param model 模型名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatQuery(String query, String model) {
        return executor.execute("chat", "-q", query, "--model", model);
    }
    /**
     * <p>执行 Hermes CLI 的 {@code chatQuery} 命令。</p>
     *
     * @param query 查询或提示词
     * @param model 模型名称
     * @param provider 模型提供方名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult chatQuery(String query, String model, String provider) {
        return executor.execute("chat", "-q", query, "--model", model, "--provider", provider);
    }

    /**
     * <p>执行 Hermes CLI 的 {@code worktreeOneShot} 命令。</p>
     *
     * @param query 查询或提示词
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult worktreeOneShot(String query) { return executor.execute("-w", "-z", query); }
    /**
     * <p>执行 Hermes CLI 的 {@code worktree} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult worktree() { return executor.execute("-w"); }

    /**
     * <p>执行 Hermes CLI 的 {@code continueSession} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult continueSession() { return executor.execute("-c"); }
    /**
     * <p>执行 Hermes CLI 的 {@code resumeSession} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult resumeSession(String id) { return executor.execute("--resume", id); }

    /** {@code hermes --tui} — TUI 模式 */

    // ============================================================
    // Model & Provider
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code model} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult model() { return executor.execute("model"); }
    /**
     * <p>执行 Hermes CLI 的 {@code model} 命令。</p>
     *
     * @param modelName 模型名称
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult model(String modelName) { return executor.execute("model", modelName); }
    /**
     * <p>执行 Hermes CLI 的 {@code fallback} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult fallback() { return executor.execute("fallback"); }

    // ============================================================
    // Setup & Config
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code setup} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult setup() { return executor.execute("setup"); }
    /**
     * <p>执行 Hermes CLI 的 {@code setupPortal} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult setupPortal() { return executor.execute("setup", "--portal"); }

    /**
     * <p>执行 Hermes CLI 的 {@code configShow} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configShow() { return executor.execute("config", "show"); }
    /**
     * <p>执行 Hermes CLI 的 {@code configEdit} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configEdit() { return executor.execute("config", "edit"); }
    /**
     * <p>执行 Hermes CLI 的 {@code configSet} 命令。</p>
     *
     * @param key 配置键
     * @param value 配置值
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configSet(String key, String value) { return executor.execute("config", "set", key, value); }
    /**
     * <p>执行 Hermes CLI 的 {@code configPath} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configPath() { return executor.execute("config", "path"); }
    /**
     * <p>执行 Hermes CLI 的 {@code configEnvPath} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configEnvPath() { return executor.execute("config", "env-path"); }
    /**
     * <p>执行 Hermes CLI 的 {@code configCheck} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configCheck() { return executor.execute("config", "check"); }
    /**
     * <p>执行 Hermes CLI 的 {@code configMigrate} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult configMigrate() { return executor.execute("config", "migrate"); }

    /**
     * <p>执行 Hermes CLI 的 {@code profile} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult profile() { return executor.execute("profile"); }

    // ============================================================
    // Gateway & Daemon
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code gateway} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gateway() { return executor.execute("gateway"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayRun} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayRun() { return executor.execute("gateway", "run"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayStart} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayStart() { return executor.execute("gateway", "start"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayStop} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayStop() { return executor.execute("gateway", "stop"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayStatus} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayStatus() { return executor.execute("gateway", "status"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayInstall} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayInstall() { return executor.execute("gateway", "install"); }
    /**
     * <p>执行 Hermes CLI 的 {@code gatewayInstallSystem} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult gatewayInstallSystem() { return executor.execute("gateway", "install", "--system"); }

    // ============================================================
    // Diagnostics
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code doctor} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult doctor() { return executor.execute("doctor"); }
    /**
     * <p>执行 Hermes CLI 的 {@code doctorFix} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult doctorFix() { return executor.execute("doctor", "--fix"); }
    /**
     * <p>执行 Hermes CLI 的 {@code status} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult status() { return executor.execute("status"); }
    /**
     * <p>执行 Hermes CLI 的 {@code statusDeep} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult statusDeep() { return executor.execute("status", "--deep"); }
    /**
     * <p>执行 Hermes CLI 的 {@code dump} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult dump() { return executor.execute("dump"); }
    /**
     * <p>执行 Hermes CLI 的 {@code logs} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult logs() { return executor.execute("logs"); }
    /**
     * <p>执行 Hermes CLI 的 {@code logs} 命令。</p>
     *
     * @param lines 日志行数
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult logs(int lines) { return executor.execute("logs", "-n", String.valueOf(lines)); }
    /**
     * <p>执行 Hermes CLI 的 {@code logsFollow} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult logsFollow() { return executor.execute("logs", "-f"); }
    /**
     * <p>查询 Hermes 基础健康状态。</p>
     *
     * <p>这是同步兼容入口，会在当前调用线程等待或执行底层 HTTP 请求。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult health() { return executor.execute("health"); }

    // ============================================================
    // Sessions
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code sessions} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessions() { return executor.execute("sessions"); }
    /**
     * <p>执行 Hermes CLI 的 {@code sessionsList} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessionsList() { return executor.execute("sessions", "list"); }
    /**
     * <p>执行 Hermes CLI 的 {@code sessionsShow} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessionsShow(String id) { return executor.execute("sessions", "show", id); }
    /**
     * <p>执行 Hermes CLI 的 {@code sessionsDelete} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessionsDelete(String id) { return executor.execute("sessions", "delete", id); }
    /**
     * <p>执行 Hermes CLI 的 {@code sessionsFork} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessionsFork(String id) { return executor.execute("sessions", "fork", id); }
    /**
     * <p>执行 Hermes CLI 的 {@code fork} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult fork(String id) { return executor.execute("fork", id); }
    /**
     * <p>执行 Hermes CLI 的 {@code sessionsRename} 命令。</p>
     *
     * @param id 对象唯一标识
     * @param title 会话标题；可以为 {@code null}
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult sessionsRename(String id, String title) { return executor.execute("sessions", "rename", id, title); }

    // ============================================================
    // Skills, Tools, Memory
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code skills} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult skills() { return executor.execute("skills"); }
    /**
     * <p>执行 Hermes CLI 的 {@code skillsList} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult skillsList() { return executor.execute("skills", "list"); }
    /**
     * <p>执行 Hermes CLI 的 {@code skillsSearch} 命令。</p>
     *
     * @param query 查询或提示词
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult skillsSearch(String query) { return executor.execute("skills", "search", query); }
    /**
     * <p>执行 Hermes CLI 的 {@code skillsInstall} 命令。</p>
     *
     * @param slug 技能标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult skillsInstall(String slug) { return executor.execute("skills", "install", slug); }
    /**
     * <p>执行 Hermes CLI 的 {@code skillsRemove} 命令。</p>
     *
     * @param slug 技能标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult skillsRemove(String slug) { return executor.execute("skills", "remove", slug); }

    /**
     * <p>执行 Hermes CLI 的 {@code tools} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult tools() { return executor.execute("tools"); }

    /**
     * <p>执行 Hermes CLI 的 {@code memory} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult memory() { return executor.execute("memory"); }

    // ============================================================
    // Cron
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code cron} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult cron() { return executor.execute("cron"); }
    /**
     * <p>执行 Hermes CLI 的 {@code cronList} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult cronList() { return executor.execute("cron", "list"); }
    /**
     * <p>执行 Hermes CLI 的 {@code cronAdd} 命令。</p>
     *
     * @param schedule Cron 表达式
     * @param prompt 定时任务提示词
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult cronAdd(String schedule, String prompt) {
        return executor.execute("cron", "add", schedule, prompt);
    }
    /**
     * <p>执行 Hermes CLI 的 {@code cronRemove} 命令。</p>
     *
     * @param id 对象唯一标识
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult cronRemove(String id) { return executor.execute("cron", "remove", id); }

    // ============================================================
    // MCP & LSP
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code mcp} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult mcp() { return executor.execute("mcp"); }
    /**
     * <p>执行 Hermes CLI 的 {@code lsp} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult lsp() { return executor.execute("lsp"); }

    // ============================================================
    // Security & Hooks
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code security} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult security() { return executor.execute("security"); }
    /**
     * <p>执行 Hermes CLI 的 {@code hooks} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult hooks() { return executor.execute("hooks"); }
    /**
     * <p>执行 Hermes CLI 的 {@code secrets} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult secrets() { return executor.execute("secrets"); }

    // ============================================================
    // Backup & Update
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code backup} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult backup() { return executor.execute("backup"); }
    /**
     * <p>执行 Hermes CLI 的 {@code update} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult update() { return executor.execute("update"); }
    /**
     * <p>执行 Hermes CLI 的 {@code uninstall} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult uninstall() { return executor.execute("uninstall"); }

    // ============================================================
    // Pairing & Login
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code pairing} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult pairing() { return executor.execute("pairing"); }
    /**
     * <p>执行 Hermes CLI 的 {@code login} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult login() { return executor.execute("login"); }
    /**
     * <p>执行 Hermes CLI 的 {@code logout} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult logout() { return executor.execute("logout"); }

    // ============================================================
    // Dashboard & UI
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code dashboard} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult dashboard() { return executor.execute("dashboard"); }
    /**
     * <p>执行 Hermes CLI 的 {@code tui} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult tui() { return executor.execute("tui"); }
    /**
     * <p>执行 Hermes CLI 的 {@code completion} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult completion() { return executor.execute("completion"); }

    // ============================================================
    // Plugins, Bundles, Curator
    // ============================================================

    /**
     * <p>执行 Hermes CLI 的 {@code plugins} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult plugins() { return executor.execute("plugins"); }
    /**
     * <p>执行 Hermes CLI 的 {@code bundles} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
    public HermesCliResult bundles() { return executor.execute("bundles"); }
    /**
     * <p>执行 Hermes CLI 的 {@code curator} 命令。</p>
     *
     * @return 包含退出码、标准输出和标准错误的命令结果
     * @since 1.0.0
     */
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
