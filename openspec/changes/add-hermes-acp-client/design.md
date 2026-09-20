# ACP 双向客户端设计

## Context

前置为已验证的 CLI 受管进程、统一可选观测与三分支兼容契约。目标是独立协议实现，而非给有限 executor 再增加一个方法。原方案 §6 规定最小双向闭环和可选依赖边界。

## Goals / Non-Goals

目标：真正初始化、会话/prompt、通知、权限、取消和退出处理；在目标能力允许时支持恢复/分叉。非目标：强迫基础用户装 ACP、复制服务端 Agent Loop、A2A/MCP、伪造 ACP 与 HTTP 的全部能力等价。

## Decisions

### 1. 先固定协议，后选择依赖

在 H-401 固定 Hermes commit 与实际 ACP 协议版本、framing、方法/错误、权限选项和能力字段，记录 contract descriptor。不能将 LSP Content-Length framing 或 CLI JSONL 直接当成 ACP。若官方/可靠 Java 协议实现满足 JDK 和依赖约束可复用；否则内部适配承担契约测试。

基础架构按可插拔 AcpCodec 与 Transport 边界设计，这使具体版本选择不改变外部 spec。未知 capability 返回不支持/未知，不猜方法字符串。目标版本尚未确定前不承诺上线支持。

### 2. 有界读写与请求关联

AcpStdioTransport 持续读 stdout，独立 stderr；单独写队列避免帧交错。JsonRpcDispatcher 用请求 ID map 关联 response；notifications 独立分发；server-to-client request 进入独立处理器。pending、frame bytes、writer queue、request timeout 全部有限。

请求 ID、session ID、原生 tool call ID 属于不同命名空间；不按同名工具或先后顺序匹配。未知 response ID 记录协议诊断，绝不随意完成 pending 中另一个请求。读取线程不等待业务 handler。

### 3. 会话状态和可选能力

Client 状态 NEW → INITIALIZING → READY → CLOSING → CLOSED/FAILED。初始化成功前拒绝 session 请求。会话分原生 server 状态与本地观察状态；prompt 串并行能力按目标协议实施，不默认允许同一会话无限并发。

会话创建、文本 prompt、事件、权限和取消组成最小首版。恢复/分叉/列出/媒体按照 negotiated capabilities gate。禁止不支持时自动 HTTP/CLI fallback；保持每个事件的 protocol=ACP 和 nativeType。

### 4. 权限回调反向请求

权限处理器异步 CompletionStage，绑定 connection generation/session/challenge/tool native identity。单次决定状态机处理 PENDING → REPLIED/DENIED/EXPIRED/CANCELLED。无 handler、失败或超时用协议认可的 deny/cancel；不假设所有协议都接受同一个 JSON body。

读循环持续处理状态查询的响应，防止 handler 为查询上下文等待读线程而死锁。迟到允许被丢弃并记录，不能覆盖已拒绝决定。长期允许只由宿主显式开放。

### 5. 进程退出与重启

exit 使所有 pending 立即异常完成，关闭写队列、超时任务和有界 stderr。owned process 按 ProcessSupervisor 回收；external ownership 不杀进程。重启增加 connection generation、重新协商，不自动 replay prompt。只在服务端支持且宿主明确要求时执行恢复。

### 6. 可选分发边界

首选在现有工程增加显式启用的独立逻辑包；核心类禁止引用可选实现以免类加载触发依赖。如果 Java 协议库最低 JDK 高于 8，则先提交可选构件设计/坐标评审，不更改核心 JDK。ACP 的支持矩阵可以小于基础 SDK，但必须明确披露且不能称三线全支持。

## Proposed public contracts

```java
public interface HermesAcpClient extends AutoCloseable {
    CompletableFuture<AcpCapabilities> initialize();
    CompletableFuture<AcpSession> createSession(AcpSessionOptions options);
    void close();
}
public interface AcpPermissionHandler {
    CompletionStage<AcpPermissionDecision> decide(AcpPermissionRequest request);
}
public interface AcpSession {
    String sessionId();
    AcpPromptHandle prompt(AcpPromptInput input);
    AutoCloseable subscribe(AcpEventListener listener);
}
```

AcpPromptHandle 保存完成 Future、显式协议取消、仅关闭观察；AcpCapabilities 保存原生协商快照和 UNKNOWN 状态；AcpPermissionRequest/Decision 保存连接 generation、会话/挑战/原生选项，不伪造缺失 server ID。AcpPromptInput 首版仅承诺已验证文本类型。可选会话操作通过明确的 capability checked 方法扩展，不把接口存在等同于远端支持。

## File changes

新增 src/main/java/io/github/easy4j/hermes/acp/HermesAcpClient.java、AcpSession.java、AcpCapabilities.java、AcpPromptHandle.java；acp/transport/AcpStdioTransport.java、AcpCodec.java；acp/rpc/JsonRpcDispatcher.java；acp/permission/AcpPermissionHandler.java、AcpPermissionRequest.java、AcpPermissionDecision.java；acp/model/AcpEvent.java。复用 cli/process/ProcessSupervisor.java 与统一观察包，不复用有限 execute。

测试增加 src/test/java/io/github/easy4j/hermes/acp/ 中的 framing、dispatcher、session、permission、exit 和 opt-in 兼容测试；可选构件路径决策在 H-401 若触发则评审明确，不未经评审拆分基础构件。

## Risks / Trade-offs

协议版本变化 → 固定 descriptor/captures，capability gate，保留未知原生类型。权限双向死锁 → 独立异步 handler 与交错请求测试。进程退出悬挂 → 集中 pending ledger 和 finally 回收。可选依赖污染核心 → 单独装载测试和字节码检查。

## Migration Plan

先交付双向初始化和请求关联，再会话/prompt，再权限/取消/异常退出，最后真实互操作及可选依赖验证。已有 cli.acp(args) 作为原始有限命令不宣称具备协议；文档引导显式 HermesAcpClient。

回滚禁用 ACP 新入口，不自动转换原 prompt 为 HTTP 重执行；保留原生会话和错误信息由宿主决定恢复。核心 HTTP/CLI 的加载与行为必须保持不变。

## Evidence gate

真实互操作必须观察 Java → Hermes 请求、Hermes → Java 权限请求、Java 权限应答及最终结果四类证据。只启动进程或只跑本地模拟器不能完成 H-404。无凭据/实例时留未完成，不用人工样本冒充真实服务通过。

## Shared implementation constraints

实现依赖必须满足本项目总计划的顺序。三条版本线分别用 Java 8、17、21 执行，不把高版本 --release 编译当作低版本真实运行。保留 Maven 坐标与常用门面，新增公开类型不暴露 Jackson 2/3 特有类型；内部序列化适配允许分支不同。所有新名称均是待实现的目标接口，不是已存在 API。

来源和目标协议必须区分：前序审计固定了 SDK commit；目标 Hermes commit 尚未选择，先通过 tasks 中的采证门禁固定版本、抓取脱敏真实样本并确认 wire 映射。行为规范规定遇到未知能力如何处理，不为未确认协议发明字段。测试数据分为官方示例、真实捕获和人工边界三类。