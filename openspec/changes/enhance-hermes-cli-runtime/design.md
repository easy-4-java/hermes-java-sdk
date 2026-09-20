# CLI 结构化执行与进程管理设计

## Context

沿用前置传输/兼容契约及 complete-hermes-http-runtime 的可选观测和事实模型；依赖顺序是本项目保守交付顺序，不意味着 CLI 在运行时依赖 HTTP 服务。现有 Commons Exec 有限执行保留。原方案 §5 提供风险与边界。

## Goals / Non-Goals

目标：进程未退出即可消费机器事件，输出和并发受控，取消和异常退出可解释。非目标：模拟交互终端、自动完成浏览器授权、安装或更新用户 Hermes、完整 ACP。

## Decisions

### 1. 不用 supplyAsync 冒充流式协议

HermesCliExecutor.execute 继续有限命令；executeStreaming 返回 HermesCliProcessHandle；startManagedProcess 返回带持续 stdin/stdout 通道的 ManagedProcess，供后续 ACP 复用。streaming stdout 按固定版本的机器格式处理；有限命令仍保留真实非零退出和受限 stdout/stderr。

### 2. 启动快照与资源准入

CliExecutionOptions 固定 executable/argv/workDir/environment/profile/home/deadline/maxOutputBytes/maxLineBytes/queueCapacity/processLimit/ownership。绝不修改全局 Profile。默认执行不通过 shell；特殊 shell 调用不纳入基础高级 argv 入口。

使用有界 admission 与输出限制。排队取消与进程退出都释放 permit。输出上限按字节处理，避免编码差异；stdout/stderr 分别有上限。超限清理所有拥有资源并返回 OutputLimitExceeded。显式 spool 是可选扩展，必须有安全目录、权限、配额和清理，不在首版默认开启。

### 3. 两路字节流，逐记录协议解析

CLI JSONL decoder 维护 CharsetDecoder 状态与尚未结束的记录；能处理中文字符跨 read、CRLF、空白与半行。只在完整记录后交付事件，达到 maxLineBytes 时立即失败；不能先 readLine 分配无限字符串。

stderr 仅供有限诊断，不喂 JSON decoder。stdout 中的非协议垃圾不当作用户答案。原生事件类型由 fixtures 固定；不要把 API SSE 事件名强塞进 CLI。旧文本 trim 保留在有限命令的兼容语义内，机器文本分片不 trim。

### 4. 成功由两个证据源联合决定

维护 startResult、protocolInitialized、terminalRecord、exitCode 四项证据。初始化前缺程序/配置/鉴权失败返回 StartupFailure；已初始化但无终态为 Interrupted/UnknownOutcome；终态声称成功但非成功退出为 Conflict，而不是静默成功。

结果文本采用已确认的最终权威文本或索引聚合，两者不能重复拼接。用量缺失保持 nullable 与来源，不从屏幕日志猜账单。

### 5. 平台进程控制与所有权

ProcessSupervisor 隐藏 OS 差异，公共 API 不用 JDK 9 ProcessHandle。默认只管 SDK 启动的进程；附着进程关闭观察不杀进程。拥有进程先尝试协议取消，宽限时间后系统终止，再检查退出与后代残留。

不使用按名称 pkill/killall 方式清理，以免误杀用户其他 Hermes。平台无法保证完整树清理时在 ProcessCapabilities 和验证报告标 LIMITED，不声称已全平台支持。

## Proposed public contracts

```java
public interface HermesCliProcessHandle extends AutoCloseable {
    CompletableFuture<CliRunResult> result();
    AutoCloseable subscribe(CliEventListener listener);
    void cancel();
    void close();
}
public interface ProcessSupervisor {
    ManagedProcess start(CliExecutionOptions options);
    ProcessCapabilities capabilities();
}
```

CliRunResult：startupStatus、nullable exitCode、protocolTerminal、completeness、finalText、typedEvents/输出引用、usage、受限 stderr、error、processDurationMillis；不持有无限完整事件历史。ManagedProcess 维护持续输入、独立输出通道、ownership、关闭与退出 Future，仅供明确受管进程使用。CliEventListener 接收原生类型及可选统一事实包络。

## File changes

修改 src/main/java/io/github/easy4j/hermes/cli/HermesCli.java、cli/HermesCliExecutor.java、HermesCliConfig.java。

新增同根目录 cli/process/CliExecutionOptions.java、ProcessSupervisor.java、ManagedProcess.java、ProcessCapabilities.java、BoundedOutputCollector.java、ProcessAdmissionController.java；cli/stream/CliStreamDecoder.java、CliEvent.java、CliRunResult.java、HermesCliProcessHandle.java；cli/availability/CliCommandCapabilities.java。全部新类型先满足 Java 8 核心模型基线。

src/test/resources/cli-fixtures/ 下新增无外部工具调用的伪 CLI。单一 shell 脚本不能作为 Windows 证据；平台测试使用对应可执行 fixture。精确测试类和行为由任务与测试目录绑定。

## Risks / Trade-offs

环境白名单可能影响已有安装 → 提供明确继承模板及缺失诊断，不能默认打印全部环境。进程树跨平台差异 → 分平台验证与声明。协议污染 → stdout 严格解析并保留 bounded stderr。输出有界可能中断长任务 → 显式容量与错误，不从日志截断推断内存有界。

## Migration Plan

先 bounded 有限执行，再 streaming decoder，最后取消与命令封装；保持 execute 的有限行为。结构化入口需要宿主明确选择目标 CLI 机器格式。旧 cronAdd 等包装先用目标 --help/解析器验证，再修其 argv 与迁移说明，不贸然删除未经确认的别名。

回滚 streaming 调用必须由业务显式切换，不在 SDK 中重跑同一任务；若任务已开始返回可核对事实。环境和 Profile 不作全局修改，无用户配置恢复操作。每个平台的进程回收独立验收。

## Evidence gate

真实 CLI version、帮助、机器格式、退出码与 Profile 选项先固定。探测不执行 install/update/delete/start 等写命令。压力用伪 CLI，不默认创建付费模型任务或开全局自动批准。

## Shared implementation constraints

实现依赖必须满足本项目总计划的顺序。三条版本线分别用 Java 8、17、21 执行，不把高版本 --release 编译当作低版本真实运行。保留 Maven 坐标与常用门面，新增公开类型不暴露 Jackson 2/3 特有类型；内部序列化适配允许分支不同。所有新名称均是待实现的目标接口，不是已存在 API。

来源和目标协议必须区分：前序审计固定了 SDK commit；目标 Hermes commit 尚未选择，先通过 tasks 中的采证门禁固定版本、抓取脱敏真实样本并确认 wire 映射。行为规范规定遇到未知能力如何处理，不为未确认协议发明字段。测试数据分为官方示例、真实捕获和人工边界三类。