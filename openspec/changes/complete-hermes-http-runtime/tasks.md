# HTTP 运行闭环实施任务

> 状态：全部待实施。规格编写完成、离线检查通过或官方 CLI 产物状态完整，都不表示下面的任务已完成。

**Goal:** 按本包 specs 的外部行为实现，保留兼容门面并交付真实证据。
**Architecture:** 采用 design.md 的职责分层和所有权/状态机，不引入无关重构。
**Tech Stack:** 现有 Maven/Java 三版本线与其依赖代际；目标 Hermes 按采证任务固定。
**Spec:** 本包 `specs/`，设计见 `design.md`，逐场景测试见 `test-catalog.md`。

## Execution notes

实现工作按独立任务执行：先读行为契约，写失败回归，记录红色原因，再最小实现与测试。提交按测试/共享逻辑/分支适配拆分；每次提交前审查 diff。示例测试命令以仓库根为工作目录，并使用该分支实际要求的 Maven/JDK；依赖不可用属于环境失败，不算行为复现。
每项数字任务均有可观察验证；H 编号用于追溯原优化方案，不替代本文件的 N.M 唯一任务编号。后续各包里的 H-501～504 是每批重复执行的发布门禁，不是额外产品范围。
业务类、测试类和 verification 路径为计划产物，当前文件包不包含伪造的 SDK 测试或实现。

### Global constraints

Java 8/17/21 三分支同契约；不以重连重做 Agent；不借用根 Profile 身份；默认不批准；外部资源不误关；任何缺失真实证据保持未完成。
### Review focus

并发关闭与新调用；相同文本不同事件；身份轮换时未知写结果；超大报文/慢消费者；权限反向请求与进程退出交错。这些边界分别绑定本包或前置包的 Scenario，不得因常规 happy-path 通过而跳过。

## 1. 请求上下文与模型快照

**原任务：** H-201。
**关联要求：** CT-001, CT-002, CT-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/context/RequestContext.java`
- `src/main/java/io/github/easy4j/hermes/context/RequestOptions.java`
- `src/main/java/io/github/easy4j/hermes/context/ModelSelection.java`
- `src/main/java/io/github/easy4j/hermes/api/model/ChatRequest.java`
- `src/main/java/io/github/easy4j/hermes/api/model/ResponseRequest.java`
- `src/main/java/io/github/easy4j/hermes/api/model/RunCreateRequest.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/context/RequestContextContractTest.java`

- [ ] 1.1 **H-201** 新增 `RequestContextContractTest` 的六场景和请求体快照断言；运行 `mvn -B -Dtest=RequestContextContractTest test`，验证并发修改、身份覆盖及媒体不支持的失败路径。
- [ ] 1.2 **H-201** 实现不可变快照、显式值优先与契约化 model/provider/options 映射；运行同类测试验证三种入口发出的有效字段一致且保留实际模型区别。
- [ ] 1.3 **H-201** 补结构化输入 builder 与安全 raw 扩展；运行 CT-003 和三线 JSON 样本对照，验证不丢未知字段、不伪造媒体接口、不覆盖凭据。

## 2. Responses 专用流及输出聚合

**原任务：** H-202。
**关联要求：** RS-001, RS-002, RS-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/ResponsesClient.java`
- `src/main/java/io/github/easy4j/hermes/api/model/ResponseEvent.java`
- `src/main/java/io/github/easy4j/hermes/api/model/OutputItem.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/ResponseAccumulator.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/api/ResponsesStreamingContractTest.java`

- [ ] 2.1 **H-202** 新增 `ResponsesStreamingContractTest` 六场景；运行 `mvn -B -Dtest=ResponsesStreamingContractTest test`，验证普通 JSON 入口拒绝 stream=true 且发送计数为零。
- [ ] 2.2 **H-202** 实现专用 stream、按原生索引输出聚合与有界结果；运行同类测试，验证工具/过程/答案分离、结果不重复累计。
- [ ] 2.3 **H-202** 覆盖空终态、中断/缺口与回调异常，接入前置取消/身份；重跑三线同样本验证完整性标志一致且无 Java 二次工具调用。

## 3. 运行句柄、幂等与真实终态

**原任务：** H-203。
**关联要求：** RN-001, RN-002, RN-003, RN-004。

**Files:**

- `src/main/java/io/github/easy4j/hermes/runtime/RunHandle.java`
- `src/main/java/io/github/easy4j/hermes/runtime/RunSnapshot.java`
- `src/main/java/io/github/easy4j/hermes/runtime/StopReceipt.java`
- `src/main/java/io/github/easy4j/hermes/api/HermesHttpClient.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/runtime/RunLifecycleContractTest.java`

- [ ] 3.1 **H-203** 新增 `RunLifecycleContractTest` 八场景，用请求计数和状态序列测试 submit/attach/timeout；运行 `mvn -B -Dtest=RunLifecycleContractTest test`，保存预期失败。
- [ ] 3.2 **H-203** 实现 submitRun/attachRun、独立 server/observation 状态及有界等待；测试 RN-001/002 验证 attach 不创建、未知状态不误判终态。
- [ ] 3.3 **H-203** 实现同一操作键/载荷/身份重试与冲突反馈；测试响应丢失和冲突，验证不换键且在无幂等契约时不自动重放。
- [ ] 3.4 **H-203** 实现 StopReceipt 与事件/查询终态核对；运行 RN-004 和自然完成竞态，验证 stop 接收后不会提前宣称 worker 已退出。

## 4. 绑定且异步的人工审批

**原任务：** H-204。
**关联要求：** AP-001, AP-002。

**Files:**

- `src/main/java/io/github/easy4j/hermes/runtime/ApprovalHandler.java`
- `src/main/java/io/github/easy4j/hermes/runtime/ApprovalRequest.java`
- `src/main/java/io/github/easy4j/hermes/runtime/ApprovalDecision.java`
- `src/main/java/io/github/easy4j/hermes/runtime/ApprovalReceipt.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/runtime/ApprovalWorkflowContractTest.java`

- [ ] 4.1 **H-204** 新增 `ApprovalWorkflowContractTest` 四场景；运行 `mvn -B -Dtest=ApprovalWorkflowContractTest test`，验证无 handler、过期和跨身份决定不批准。
- [ ] 4.2 **H-204** 实现异步挑战状态与 wire 映射，绑定运行/身份/原生挑战信息；运行同类测试，验证有效决定只发送一次且审批回调不阻塞事件读取。
- [ ] 4.3 **H-204** 测试超时迟到决定、重复提交和无 server challenge ID 的保守路径；验证未伪造 exactly-once，Receipt 不被当成 Run 成功。

## 5. 模型、会话、Jobs 与能力

**原任务：** H-205。
**关联要求：** RR-001, RR-002, RR-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/ModelsClient.java`
- `src/main/java/io/github/easy4j/hermes/api/SessionsClient.java`
- `src/main/java/io/github/easy4j/hermes/api/JobsClient.java`
- `src/main/java/io/github/easy4j/hermes/api/CapabilitiesClient.java`
- `src/main/java/io/github/easy4j/hermes/api/HealthClient.java`
- `src/main/java/io/github/easy4j/hermes/api/model/FieldUpdate.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/api/RuntimeResourcesContractTest.java`

- [ ] 5.1 **H-205** 新增 `RuntimeResourcesContractTest` 六场景及契约确认表；运行 `mvn -B -Dtest=RuntimeResourcesContractTest test`，验证未支持/未知能力不被当作可用。
- [ ] 5.2 **H-205** 补类型化模型选项元数据、会话分页与历史请求；用已固定 wire 样本测试边界和扩展保留，验证必要 async 入口取消语义一致。
- [ ] 5.3 **H-205** 实现 Job 创建/增量修改/控制与 UNSET/SET/CLEAR；测试省略与清空序列化不同，立即执行不默认重放且无额外本地调度。
- [ ] 5.4 **H-205** 实现能力隔离缓存与 Health 分类；运行 RR-003，验证鉴权失败不等于离线、能力中的新端点不获自动授权。

## 6. 错误、响应元数据和安全观测

**原任务：** H-206。
**关联要求：** OB-001, OB-002, OB-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/model/ApiResponse.java`
- `src/main/java/io/github/easy4j/hermes/exception/HermesError.java`
- `src/main/java/io/github/easy4j/hermes/api/model/UsageSnapshot.java`
- `src/main/java/io/github/easy4j/hermes/observability/RuntimeObserver.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/observability/RuntimeObservabilityContractTest.java`

- [ ] 6.1 **H-206** 新增 `RuntimeObservabilityContractTest` 六场景；运行 `mvn -B -Dtest=RuntimeObservabilityContractTest test`，验证错误分类、缺失用量和秘密日志边界。
- [ ] 6.2 **H-206** 实现状态/必要响应头/受限错误体和重试决策，增加新严格删除结果；验证 404/403/500 不混淆，Retry-After 被 deadline 约束。
- [ ] 6.3 **H-206** 实现 usage/runtime 事实模型与可选 observer；测试重复终态不倍增用量、缺失不伪造零、无遥测后端仍正常工作。
- [ ] 6.4 **H-206** 对 BODY、原始报文诊断和观察者异常加入脱敏/长度/隔离检查；运行 OB-003 并审核输出，验证无秘密和无限高基数标签。

## 7. HTTP 批次兼容与互操作门禁

**原任务：** H-501, H-502, H-503, H-504。
**关联要求：** 本包全部要求及前置 sdk-compatibility 发布门禁。

**Files:**

- `docs/compatibility/http-runtime-migration.md`
- `verification/http-runtime/`
- `README.md`
- `README.zh-CN.md`

- [ ] 7.1 **H-501** 对当前新增 API 和同版样本运行三线 guard 与真实 JDK 的 `mvn -B --no-transfer-progress clean verify`；验证全部新增业务结果等价。
- [ ] 7.2 **H-502** 在固定授权测试 Hermes 上执行创建→事件→审批→停止→真实终态；验证每步有脱敏证据和资源收敛，无实例时本项保持未完成。
- [ ] 7.3 **H-503** 交付 Responses、Run、审批、Jobs 和取消迁移示例；核对配置/方法与实现一致，不把估算费用写成真实账单。
- [ ] 7.4 **H-504** 汇总本包证据并复核前置包已实施/归档；验证声明支持范围、diff、文档和未完成项一致后才批准候选发布。