# HTTP 运行闭环设计

## Context

以 harden-hermes-transport 中的身份、端点、取消、事件和三分支契约为前置；不是先在不可靠 SSE 之上实现 RunHandle。已有 Chat/Responses/Run/Session/Jobs 包装保留兼容入口，实际职责逐步委托。参见 proposal.md 与原方案 §4、§8。

## Goals / Non-Goals

目标：完整表达提交、观察、审批、停止、核对终态及事实性用量。非目标：重新实现 Agent Loop、平台授权、费用定价、数据库调度或媒体 API 全集。

## Decisions

### 1. 三种请求值对象和不可变快照

RequestContext 管身份、sessionId、sessionKey、correlationId；RequestOptions 管 deadline/cancellation/idempotencyKey/允许 headers；ModelSelection 管 model/provider/modelOptions。合并器在构造 wire request 前生成深度快照，不能保留会被另一线程修改的 Map/List。

只在目标端点确认支持时映射 provider/model_options；没确认时拒绝高层使用或返回 UNKNOWN，不杜撰字段。已有请求可使用新增重载，不要求批量修改调用方。特殊 null 语义使用显式选择对象，而非把默认、未提供和清空混为一谈。

### 2. Responses 作为独立流式产品接口

ResponsesClient 分 JSON 与 stream 两条入口。JSON 入口提前拒绝 stream=true。ResponseAccumulator 使用原生 itemId/outputIndex/contentIndex 的关联键；文本、工具、过程信息独立。真实 schema 无某索引时使用其声明的替代关联，不自造服务端 ID。

StreamingResult<T> 保存 terminalStatus、complete、partialValue、usage、runtime 和诊断。业务失败保留失败，不被 transport close 覆盖。多个 output、空输出、重复终态、乱序工具事件及无法证明连续性均有测试。服务端工具记录只用于展示/审计，不发起 Java 工具执行。

### 3. RunHandle 是观察句柄，不是持久工作流

submitRun 与 attachRun 返回同一句柄。内部运行快照与观察状态两张状态表独立：serverStatus/rawStatus 不由本地超时更改；observationStatus 可以 CONNECTED、DISCONNECTED、TIMED_OUT、CLOSED。

幂等记录用 endpointIdentity/profile/credentialIdentity/key/payloadHash 标识操作，同一重试复用 payload 字节与 key。持久化由宿主完成，不把本地缓存宣称跨进程 exactly-once。404 保留不可见/不存在的不确定范围。

requestStop 返回 StopReceipt，不直接完成 terminal future；awaitTerminal 在 deadline 内消费事件或有界轮询。轮询 executor 与回调隔离，退避被 deadline 截断，绝不隐式 stop。

### 4. 审批是带绑定的异步控制

ApprovalHandler 返回 CompletionStage<ApprovalDecision>；挂起挑战以服务端提供的原生身份关联到 endpoint/profile/run，缺失时保守处理而不虚构 server-side 防重。挑战状态 PENDING → DECIDED / EXPIRED / CANCELLED；CAS 限制本地单次应答。

没有 handler、超时或异常都不发允许；协议支持拒绝则发拒绝，否则明确保留未解决或取消控制的结果。决定提交 Receipt 不等于运行成功。不要阻塞 SSE 读取线程等待人工。

### 5. 领域资源与可演进模型

ModelsClient、SessionsClient、JobsClient、CapabilitiesClient、HealthClient 为小型逻辑客户端，旧 HermesClient 门面委托。Jobs Patch 使用 FieldUpdate<T>：UNSET/SET(value)/CLEAR，按服务端实际 PATCH/POST 契约序列化。分页保存 continuation/offset 信息，不隐式取全量。

CapabilitiesSnapshot 分 SUPPORTED/UNSUPPORTED/UNKNOWN，并保留来源和受控缓存；服务端 capability 不能修改客户端信任 origin。目标模型选择接口的路径和字段在 H-205 固定，文档提及的候选 `/api/model/options` 不能仅凭路径常量视为已联调。

### 6. 错误与观测接口不绑定后端

ApiResponse<T> 保存状态和必要响应元数据；HermesError 保存 category/serverCode/requestId/retryAfter/boundedBody/unknownOutcome。RetryDecider 联合操作语义、提交阶段与目标契约，不仅检查异常类型。

UsageSnapshot 保留 nullable token/cache 值和来源；RuntimeSelection 分 request 与 actual。RuntimeObserver 只传受控事件，适配 OpenTelemetry/Micrometer 另做可选模块。默认无 BODY，敏感字段脱敏、长度和标签基数受限；rawData 可读并不自动写文件。

## Proposed public contracts

```java
public interface RunHandle extends AutoCloseable {
    String runId();
    CompletableFuture<RunSnapshot> snapshotAsync();
    AutoCloseable subscribe(RunEventListener listener);
    CompletableFuture<ApprovalReceipt> approve(ApprovalDecision decision);
    CompletableFuture<StopReceipt> requestStop();
    CompletableFuture<RunSnapshot> awaitTerminal(long timeoutMillis);
    void close(); // 仅关闭本地观察
}
public interface ApprovalHandler {
    CompletionStage<ApprovalDecision> decide(ApprovalRequest request);
}
public interface RuntimeObserver {
    void onEvent(RuntimeObservation observation);
}
```

RunSnapshot：runId、rawStatus、knownStatus、observationStatus、usage、runtime、error、可用原生时间。RunEventListener 接收统一包络和端点原生事件。StopReceipt：请求是否接收及受限响应元数据，不承诺终态。ApprovalRequest/Decision/Receipt：绑定身份、原生挑战信息、决定、提交状态。RequestContext/Options/ModelSelection 字段见上文，所有新增模型与基础 API 均使用 Java 8 可表达类型。

## File changes

新增 context/RequestContext.java、context/RequestOptions.java、context/ModelSelection.java、runtime/RunHandle.java、runtime/RunSnapshot.java、runtime/ApprovalHandler.java、api/ResponsesClient.java、api/ModelsClient.java、api/SessionsClient.java、api/JobsClient.java、api/CapabilitiesClient.java、api/HealthClient.java、api/model/FieldUpdate.java、api/model/ApiResponse.java、exception/HermesError.java、observability/RuntimeObserver.java。所有路径均以 src/main/java/io/github/easy4j/hermes/ 为根。

修改 HermesClient.java、api/HermesHttpClient.java、api/model/ChatRequest.java、ResponseRequest.java、RunCreateRequest.java、ResponseResult.java、RunStatus.java、CapabilityInfo.java；保留旧入口，由新组件委托，JSON 适配在分支内部完成。

## Risks / Trade-offs

类型化模型落后于服务端 → 保留受控原始扩展和 UNKNOWN，不随意吞掉未知控制语义。幂等契约因版本不同 → 在提交前能力门禁，不开通无依据写重试。审批异步迟到 → 单次决定状态机与挑战范围，不能让迟到允许覆盖拒绝。回调失败 → 分类与隔离，终态清理不依赖回调成功。

## Migration Plan

先完成 RequestContext/错误结果，再增 Responses 与 RunHandle，随后审批和资源客户端。业务应用可逐个使用新入口；旧 boolean 删除继续明确旧语义，新严格类型化入口不退化为布尔值。迁移同时演示“cancel observation / stop remote”的区别。

回滚新入口调用方到已验证 JSON/CLI 有限入口必须由用户主动选择，SDK 不自动 fallback 重执行；已经创建的 Run 继续按原身份附着。无服务端数据迁移。每批候选三线通过后才归档本变更。

## Evidence gate

前置包须具备真实传输证据。Run 幂等、终态、审批 wire body、输出索引与模型选项按目标 Hermes capture 固定；尚未确认的路径和能力维持 UNKNOWN，不作为交付支持。性能阈值从固定工作负载基线冻结，不预设无依据吞吐数字。

## Shared implementation constraints

实现依赖必须满足本项目总计划的顺序。三条版本线分别用 Java 8、17、21 执行，不把高版本 --release 编译当作低版本真实运行。保留 Maven 坐标与常用门面，新增公开类型不暴露 Jackson 2/3 特有类型；内部序列化适配允许分支不同。所有新名称均是待实现的目标接口，不是已存在 API。

来源和目标协议必须区分：前序审计固定了 SDK commit；目标 Hermes commit 尚未选择，先通过 tasks 中的采证门禁固定版本、抓取脱敏真实样本并确认 wire 映射。行为规范规定遇到未知能力如何处理，不为未确认协议发明字段。测试数据分为官方示例、真实捕获和人工边界三类。