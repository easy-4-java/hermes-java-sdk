# ACP 双向协议客户端实施任务

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

## 1. 协议固定、双向 transport 与 dispatcher

**原任务：** H-401。
**关联要求：** AT-001, AT-002。

**Files:**

- `src/main/java/io/github/easy4j/hermes/acp/transport/AcpCodec.java`
- `src/main/java/io/github/easy4j/hermes/acp/transport/AcpStdioTransport.java`
- `src/main/java/io/github/easy4j/hermes/acp/rpc/JsonRpcDispatcher.java`
- `docs/compatibility/acp-contract.md`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/acp/AcpTransportContractTest.java`

- [ ] 1.1 **H-401** 固定 Hermes/ACP 版本、framing、能力字段、方法及错误，决定满足 JDK 的协议适配；验证 descriptor 有来源与字节码结论，不假设 CLI/LSP framing。
- [ ] 1.2 **H-401** 新增 `AcpTransportContractTest` 的 AT-001/002 四场景：真实帧、乱序响应、通知与未知 ID；运行 `mvn -B -Dtest=AcpTransportContractTest test` 记录失败。
- [ ] 1.3 **H-401** 实现持续读写、初始化、请求关联及通知分流；运行同类测试，验证初始化前不发 session 请求且 stderr 不入协议 stdout。
- [ ] 1.4 **H-401** 实现 frame/pending/writer/deadline 上限和超限清理；负载测试验证无无限请求表、乱序响应不误完成其他请求。

## 2. 会话与能力受控的 prompt

**原任务：** H-402。
**关联要求：** AS-001, AS-002, AS-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/acp/HermesAcpClient.java`
- `src/main/java/io/github/easy4j/hermes/acp/AcpSession.java`
- `src/main/java/io/github/easy4j/hermes/acp/AcpPromptHandle.java`
- `src/main/java/io/github/easy4j/hermes/acp/AcpCapabilities.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/acp/AcpSessionsContractTest.java`

- [ ] 2.1 **H-402** 新增 `AcpSessionsContractTest` 六场景及会话状态断言；运行 `mvn -B -Dtest=AcpSessionsContractTest test`，验证创建/prompt/取消和不支持能力路径。
- [ ] 2.2 **H-402** 实现最小 session/prompt/事件/结果与显式取消；运行 AS-001，验证原生会话关联、进度先于完成且其他会话不受影响。
- [ ] 2.3 **H-402** 对恢复/分叉/媒体和同名工具关联增加 capability gate；运行 AS-002，验证不自动 HTTP fallback、不用名称猜原生调用 ID。
- [ ] 2.4 **H-402** 实现显式启用与可选依赖隔离；运行 AS-003 和 Java 8 类加载检查，无 ACP 环境时基础 HTTP/CLI 不探测、不报依赖缺失。

## 3. 权限反向请求与异常退出

**原任务：** H-403。
**关联要求：** AM-001, AM-002, AT-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/acp/permission/AcpPermissionHandler.java`
- `src/main/java/io/github/easy4j/hermes/acp/permission/AcpPermissionRequest.java`
- `src/main/java/io/github/easy4j/hermes/acp/permission/AcpPermissionDecision.java`
- `src/main/java/io/github/easy4j/hermes/acp/rpc/JsonRpcDispatcher.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/acp/AcpPermissionsContractTest.java`
- `src/test/java/io/github/easy4j/hermes/acp/AcpTransportContractTest.java`

- [ ] 3.1 **H-403** 新增 `AcpPermissionsContractTest` 四场景和 `AcpTransportContractTest` 退出/重启场景；运行两类，验证异步回调查询状态时读循环不死锁。
- [ ] 3.2 **H-403** 实现连接代次/会话/挑战绑定、默认拒绝、超时和单次应答；测试迟到允许被忽略、过期授权不能用于新连接。
- [ ] 3.3 **H-403** 将进程退出连接到 pending ledger 和任务清理；运行 AT-003，验证全部挂起请求结束且重启不会自动 replay prompt。
- [ ] 3.4 **H-403** 覆盖协议取消/权限拒绝/进程退出竞态及外部进程 ownership；运行当前权限和传输测试及资源检查，验证无双应答、误杀或永久等待。

## 4. 真实 ACP 互操作与示例

**原任务：** H-404。
**关联要求：** AT-001, AT-003, AS-001, AM-002。

**Files:**

- `src/test/resources/contracts/acp/`
- `docs/examples/acp-runtime.md`
- `verification/acp-interop/`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/acp/AcpPermissionsContractTest.java`
- `src/test/java/io/github/easy4j/hermes/acp/AcpSessionsContractTest.java`
- `src/test/java/io/github/easy4j/hermes/acp/AcpTransportContractTest.java`

- [ ] 4.1 **H-404** 建立已授权目标 Hermes 的最小互操作环境，记录版本、选项及只读/审批控制测试工具；验证无全局自动批准，无凭据时不伪造环境通过。
- [ ] 4.2 **H-404** 执行 Java 初始化→建会话→prompt→服务端权限反向请求→Java 决定→终态；验证四向消息、请求 ID 和最终结果均有脱敏证据。
- [ ] 4.3 **H-404** 执行中途取消、对端退出和显式恢复/不支持恢复分支；验证挂起请求结束、无自动重放，并保存资源回收证据。
- [ ] 4.4 **H-404** 编写显式启用/权限处理/关闭的 Java 示例并在宣称支持的版本线编译运行；验证基础 SDK 不装 ACP 仍可运行。

## 5. 可选协议发布和整体闭环

**原任务：** H-501, H-502, H-503, H-504。
**关联要求：** 本包全部要求及前置 sdk-compatibility 发布门禁。

**Files:**

- `docs/compatibility/acp-migration.md`
- `verification/acp-release/`
- `README.md`
- `README.zh-CN.md`

- [ ] 5.1 **H-501** 运行三线公开契约、真实 JDK 和核心无 ACP 类加载测试；验证可选依赖矩阵清晰，不把有限 ACP 支持写成所有版本线等价支持。
- [ ] 5.2 **H-502** 执行双向交错、frame/pending 饱和、取消/退出故障注入；验证有界资源和权限读循环存活并记录结果。
- [ ] 5.3 **H-503** 更新 ACP 支持矩阵、迁移、权限和原 cli.acp 边界；检视全部四包的文档和实现符合 spec。
- [ ] 5.4 **H-504** 按依赖复核四包与原 28 项任务及 28 个验收场景的证据，不遗漏未验证项；只有授权评审及真实测试齐全后才发布并归档，不自动启用 A2A/MCP/Admin。