# 传输、安全与兼容性加固实施任务

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

## 1. 建立源码、协议与失败基线

**原任务：** H-001, H-002, H-003, H-004。
**关联要求：** BC-002。

**Files:**

- `verification/source-manifest.json`
- `verification/server-compatibility.json`
- `src/test/resources/contracts/manifest.json`
- `docs/compatibility/hermes-contract-matrix.md`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/compatibility/SdkCompatibilityContractTest.java`

- [ ] 1.1 **H-001** 记录三分支 HEAD、POM、Maven/真实 JDK、依赖字节码及宿主授权的目标 Hermes commit；验证 `git rev-parse HEAD`、`java -version`、`mvn -version` 与清单一致，未提供的服务器不得填造。
- [ ] 1.2 **H-002** 建立端点/CLI/ACP 能力与 wire 映射登记表；验证每项有 SUPPORTED/UNSUPPORTED/UNKNOWN、版本和证据路径，能力未知不进入自动执行。
- [ ] 1.3 **H-003** 新增脱敏协议样本及 SHA-256 manifest，区分真实捕获/官方示例/人工边界；验证样本可解析、秘密扫描为零且每个来源可追溯。
- [ ] 1.4 **H-004** 从公开入口新增标准 chunk、Session POST 断线及公开 Future 取消回归；运行 `mvn -B -Dtest=SseLifecycleContractTest,PublicCancellationContractTest test`（逐场景断言见 test-catalog.md），记录三线预期失败或静态证据，不将构建环境失败误当行为复现。

## 2. 可信端点和实际连接策略

**原任务：** H-101。
**关联要求：** EP-001, EP-002, EP-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/security/EndpointPolicy.java`
- `src/main/java/io/github/easy4j/hermes/util/EndpointGuard.java`
- `src/main/java/io/github/easy4j/hermes/HermesHttpClientConfig.java`
- `src/main/java/io/github/easy4j/hermes/HermesOkHttpClientFactory.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/security/EndpointPolicyContractTest.java`

- [ ] 2.1 **H-101** 新增 `EndpointPolicyContractTest` 的六个场景；运行 `mvn -B -Dtest=EndpointPolicyContractTest test`，确认未修复时至少相关行为回归失败，断言私网/跳转在凭据发送前拒绝。
- [ ] 2.2 **H-101** 实现显式本地/私网/公网策略、全部候选地址校验、origin 与路径段约束并接入 HTTP/SSE；运行同一测试类，验证可信地址成功与混合 DNS/跨域拒绝同时通过。
- [ ] 2.3 **H-101** 更新可信本地示例并弃用生产中的测试旁路；检视公开差异与示例，验证现有公开签名未被直接删除且示例不再调用测试开关。

## 3. Profile 凭据及缓存隔离

**原任务：** H-102。
**关联要求：** PA-001, PA-002, PA-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/security/ProfileBinding.java`
- `src/main/java/io/github/easy4j/hermes/security/CredentialProvider.java`
- `src/main/java/io/github/easy4j/hermes/security/ProfileCredentialResolver.java`
- `src/main/java/io/github/easy4j/hermes/HermesClient.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/security/ProfileAuthenticationContractTest.java`

- [ ] 3.1 **H-102** 新增 `ProfileAuthenticationContractTest` 六个场景并使用两个不同假密钥；运行该类，验证根凭据借用、覆盖头与轮换行为有可复现断言。
- [ ] 3.2 **H-102** 实现每次请求凭据解析、非秘密身份缓存键和保护头校验；运行 `mvn -B -Dtest=ProfileAuthenticationContractTest test`，验证并发请求无身份串用。
- [ ] 3.3 **H-102** 覆盖外部有状态 interceptor、CookieJar、缓存与 resolver 缺失；运行同一类并审核日志，验证拒绝或隔离策略明确、密钥不入缓存键/输出。

## 4. SSE 原始帧及端点解码

**原任务：** H-103。
**关联要求：** SE-001, SE-002。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/HermesSseClient.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/SseFrame.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/EndpointEventDecoder.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/SseEvent.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/api/sse/SseLifecycleContractTest.java`

- [ ] 4.1 **H-103** 在 `SseLifecycleContractTest` 新增 SE-001/002 场景：标准 chunk、多行 data、UTF-8 分片、回调抛错；运行 `mvn -B -Dtest=SseLifecycleContractTest test` 并保留修复前失败原因。
- [ ] 4.2 **H-103** 实现 raw frame→端点 decoder→类型化事件与旧 SseEvent 适配；运行该测试，验证无需额外 JSON data 包装、无伪造服务端 ID。
- [ ] 4.3 **H-103** 分离解析/网络/回调/业务错误并核对帧大小保护；重跑对应场景和超大帧边界，验证消费者异常不会伪装成解析错误且资源仍释放。

## 5. 终态、幂等重试及重新附着

**原任务：** H-104。
**关联要求：** SE-003, SE-004, SE-005。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/HermesSseClient.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/StreamingChatResponse.java`
- `src/main/java/io/github/easy4j/hermes/transport/RequestSemantics.java`
- `src/main/java/io/github/easy4j/hermes/transport/RetryPolicy.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/api/sse/SseLifecycleContractTest.java`

- [ ] 5.1 **H-104** 新增 SE-003～005 六个场景，以服务端请求计数断言“终态后 EOF”和“中途断线”都只有一次聊天 POST；运行 `mvn -B -Dtest=SseLifecycleContractTest test` 验证旧风险。
- [ ] 5.2 **H-104** 实现端点终态、至多一次完成、EOF 核对与按请求语义限制底层/上层重试；运行同类测试，验证无依据写重放为零。
- [ ] 5.3 **H-104** 实现既有运行 reattach、连续性标志及原生 ID 去重；测试相同文本不同事件不被删除、无游标恢复标 UNVERIFIED，不生成新 Run。

## 6. 有界有序派发与慢消费者

**原任务：** H-105。
**关联要求：** SE-006。

**Files:**

- `src/main/java/io/github/easy4j/hermes/api/sse/OrderedEventDispatcher.java`
- `src/main/java/io/github/easy4j/hermes/api/sse/SseQueueSubscription.java`
- `src/main/java/io/github/easy4j/hermes/api/HermesSseClient.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/api/sse/SseLifecycleContractTest.java`

- [ ] 6.1 **H-105** 新增 SE-006 场景并设置容量为 1，制造慢消费者与控制事件；运行 `mvn -B -Dtest=SseLifecycleContractTest test`，验证饱和可检测。
- [ ] 6.2 **H-105** 实现按订阅顺序派发、默认 FAIL 溢出和显式进度合并；运行同类及有界负载，验证文本/审批/终态不静默丢弃。
- [ ] 6.3 **H-105** 覆盖两个订阅一慢一快、关闭时排队事件和重连任务；验证 B 不被 A 阻塞、queue/线程归零或回到所记录基线。

## 7. 公开取消和资源关闭竞态

**原任务：** H-106。
**关联要求：** RC-001, RC-002, RC-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/transport/CancellationContext.java`
- `src/main/java/io/github/easy4j/hermes/transport/TransportOwnership.java`
- `src/main/java/io/github/easy4j/hermes/api/HermesHttpClient.java`
- `src/main/java/io/github/easy4j/hermes/HermesClient.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/transport/PublicCancellationContractTest.java`

- [ ] 7.1 **H-106** 新增 `PublicCancellationContractTest` 六个场景，从 SDK 直接返回结果发起 cancel；运行 `mvn -B -Dtest=PublicCancellationContractTest test`，验证底层调用取消而其他请求正常。
- [ ] 7.2 **H-106** 绑定外层 Future、显式令牌、队列与重试到取消上下文；重跑该类，验证未开始任务不会发送、注册均注销。
- [ ] 7.3 **H-106** 实现所有权账本、幂等 close 和创建/关闭并发门禁；用外部共享客户端与并发 subscribe 测试，验证无关 Call 不被 cancelAll、无遗留订阅。
- [ ] 7.4 **H-106** 将观察关闭与远端停止拆开并更新示例；验证 RC-002 两场景远端 stop 计数为零，已发写请求被取消时暴露未知结果。

## 8. 三分支契约及第一批发布门禁

**原任务：** H-501, H-502, H-503, H-504。
**关联要求：** BC-001, BC-002, BC-003。

**Files:**

- `.github/workflows/ci.yml`
- `scripts/verify-api-shape.py`
- `scripts/verify-branch-contracts.py`
- `docs/compatibility/transport-migration.md`
- `verification/branch-sync-report.json`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/compatibility/SdkCompatibilityContractTest.java`

- [ ] 8.1 **H-501** 新增契约/API Shape/共享源码范围及显式白名单检查；运行脚本与 `SdkCompatibilityContractTest`，验证三线公共语义一致，差异只有已审查适配项。
- [ ] 8.2 **H-501** 在实际 JDK 8/17/21 分别运行 `mvn -B --no-transfer-progress clean verify` 并检查依赖字节码；验证报告记录真实环境而非仅 --release 编译。
- [ ] 8.3 **H-502** 使用可控 HTTP/SSE 服务执行 100/300/500/800/1000 档位及断线/慢消费者/取消测试；验证事件完整性与资源收敛，阈值按固定基线冻结，不调用大量付费 Agent。
- [ ] 8.4 **H-503** 更新 README 的 POM 对齐矩阵、Profile/端点/取消/重连迁移和日志限制；检查示例与新行为一致、无“无感兼容”误导或正文秘密泄露。
- [ ] 8.5 **H-504** 汇总当前批次代码 diff、三线测试和真实目标互操作证据；只有所有支持声明有证据时才批准候选发布/归档，本任务不得因规范文件已写完而勾选。