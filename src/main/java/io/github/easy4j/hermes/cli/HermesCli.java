package io.github.easy4j.hermes.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地 {@code hermes} CLI 命令封装。
 */
public class HermesCli {

    private static final Logger log = LoggerFactory.getLogger(HermesCli.class);

    private final HermesCliExecutor executor;

    public HermesCli(HermesCliExecutor executor) {
        this.executor = executor;
    }

    /**
     * 获取 CLI 执行器（用于自定义命令）。
     */
    public HermesCliExecutor executor() {
        return executor;
    }

    /**
     * {@code hermes --version}
     */
    public HermesCliResult version() {
        return executor.execute("--version");
    }

    // ============================================================
    // Chat
    // ============================================================

    /**
     * {@code hermes chat -q "<query>"}
     * <p>非交互模式发送 chat query，返回 AI 响应。</p>
     */
    public HermesCliResult chatQuery(String query) {
        return executor.execute("chat", "-q", query);
    }

    /**
     * {@code hermes chat -q "<query>" --model <model>}
     */
    public HermesCliResult chatQuery(String query, String model) {
        return executor.execute("chat", "-q", query, "--model", model);
    }

    /**
     * {@code hermes chat -q "<query>" --model <model> --provider <provider>}
     */
    public HermesCliResult chatQuery(String query, String model, String provider) {
        return executor.execute("chat", "-q", query, "--model", model, "--provider", provider);
    }

    /**
     * {@code hermes -z "<query>"}
     * <p>One-shot 模式：发送 query 后立即退出。</p>
     */
    public HermesCliResult chatOneShot(String query) {
        return executor.execute("-z", query);
    }

    /**
     * {@code hermes -z "<query>" --model <model>}
     */
    public HermesCliResult chatOneShot(String query, String model) {
        return executor.execute("-z", query, "--model", model);
    }

    // ============================================================
    // Setup
    // ============================================================

    /**
     * {@code hermes setup --portal}
     */
    public HermesCliResult setupPortal() {
        return executor.execute("setup", "--portal");
    }

    // ============================================================
    // Gateway
    // ============================================================

    /**
     * {@code hermes gateway start}
     */
    public HermesCliResult gatewayStart() {
        return executor.execute("gateway", "start");
    }

    /**
     * {@code hermes gateway stop}
     */
    public HermesCliResult gatewayStop() {
        return executor.execute("gateway", "stop");
    }

    /**
     * {@code hermes gateway status}
     */
    public HermesCliResult gatewayStatus() {
        return executor.execute("gateway", "status");
    }

    // ============================================================
    // Diagnostics
    // ============================================================

    /**
     * {@code hermes doctor}
     */
    public HermesCliResult doctor() {
        return executor.execute("doctor");
    }

    /**
     * {@code hermes dump}
     */
    public HermesCliResult dump() {
        return executor.execute("dump");
    }

    // ============================================================
    // Sessions & Skills
    // ============================================================

    /**
     * {@code hermes sessions list}
     */
    public HermesCliResult sessionsList() {
        return executor.execute("sessions", "list");
    }

    /**
     * {@code hermes skills list}
     */
    public HermesCliResult skillsList() {
        return executor.execute("skills", "list");
    }

    // ============================================================
    // Status
    // ============================================================

    /**
     * {@code hermes status}
     */
    public HermesCliResult status() {
        return executor.execute("status");
    }
}
