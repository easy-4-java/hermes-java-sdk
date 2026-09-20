# CLI 结构化执行与进程管理实施任务

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

## 1. 有限/流式/受管执行与资源准入

**原任务：** H-301。
**关联要求：** CE-001, CE-002, CE-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/cli/process/CliExecutionOptions.java`
- `src/main/java/io/github/easy4j/hermes/cli/process/ProcessAdmissionController.java`
- `src/main/java/io/github/easy4j/hermes/cli/process/BoundedOutputCollector.java`
- `src/main/java/io/github/easy4j/hermes/cli/HermesCliExecutor.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/cli/process/CliExecutionContractTest.java`

- [ ] 1.1 **H-301** 新增 `CliExecutionContractTest` 的 CE-001～003 场景和无副作用测试进程；运行 `mvn -B -Dtest=CliExecutionContractTest test`，断言 argv、环境、双 Profile 及排队限制。
- [ ] 1.2 **H-301** 实现每次启动快照、允许环境和独立 argv；运行对应场景，验证父环境/全局 Profile 未修改且 shell 字符未执行。
- [ ] 1.3 **H-301** 实现有限输出上限、并发 permit 和有界可取消排队；运行输出洪泛/取消场景，验证内存及配额回归记录基线。
- [ ] 1.4 **H-301** 保留旧有限 execute 并增加受管启动边界；测试交互命令明确拒绝或委托，验证不将有限入口变成长连接。

## 2. CLI JSONL 与结果核对

**原任务：** H-302。
**关联要求：** CS-001, CS-002, CS-003。

**Files:**

- `src/main/java/io/github/easy4j/hermes/cli/stream/CliStreamDecoder.java`
- `src/main/java/io/github/easy4j/hermes/cli/stream/CliEvent.java`
- `src/main/java/io/github/easy4j/hermes/cli/stream/CliRunResult.java`
- `src/main/java/io/github/easy4j/hermes/cli/stream/HermesCliProcessHandle.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/cli/stream/CliStreamingContractTest.java`

- [ ] 2.1 **H-302** 新增 `CliStreamingContractTest` 七场景，覆盖字节分片、stderr、启动失败和终态冲突；运行 `mvn -B -Dtest=CliStreamingContractTest test` 验证缺陷边界。
- [ ] 2.2 **H-302** 实现有界增量 UTF-8 JSONL 解码和 stdout/stderr 分离；重跑测试，验证半行只在完整后交付、长行超限不先分配无限内存。
- [ ] 2.3 **H-302** 实现 live handle 与独立终态/exit 聚合；测试进程未退出前事件可见、部分文本不报成功、重复最终文本不双拼。
- [ ] 2.4 **H-302** 按固定 CLI 样本映射机器事件和事实用量，接入可选 observer；验证未知类型保留、缺字段不猜测，秘密不被日志外送。

## 3. 取消、超时与平台资源回收

**原任务：** H-303。
**关联要求：** CE-004。

**Files:**

- `src/main/java/io/github/easy4j/hermes/cli/process/ProcessSupervisor.java`
- `src/main/java/io/github/easy4j/hermes/cli/process/ManagedProcess.java`
- `src/main/java/io/github/easy4j/hermes/cli/process/ProcessCapabilities.java`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/cli/process/CliExecutionContractTest.java`

- [ ] 3.1 **H-303** 为 CE-004 新增忽略取消和外部附着场景，并加入受控后代进程；运行 `mvn -B -Dtest=CliExecutionContractTest test`，记录修复前资源状态。
- [ ] 3.2 **H-303** 实现协议取消→宽限等待→系统终止→退出确认，只作用于拥有进程；重跑场景并检查后代残留，不使用按名称全局 kill。
- [ ] 3.3 **H-303** 分别在 Linux/macOS/Windows 运行平台 fixture 并记录 JDK/OS、PID 与退出证据；未验证平台标 LIMITED/UNVERIFIED，不复制其他平台通过结果。
- [ ] 3.4 **H-303** 验证重复 close、排队取消、stderr 洪泛和超时竞态都释放 permit/Future/线程；重跑资源压力测试，确认结果回到固定基线。

## 4. 命令契约与结构化用量

**原任务：** H-304。
**关联要求：** CC-001, CC-002。

**Files:**

- `src/main/java/io/github/easy4j/hermes/cli/HermesCli.java`
- `src/main/java/io/github/easy4j/hermes/cli/availability/CliCommandCapabilities.java`
- `docs/compatibility/cli-command-matrix.md`

**Test files（新增或扩展）：**

- `src/test/java/io/github/easy4j/hermes/cli/CliCommandsContractTest.java`

- [ ] 4.1 **H-304** 用目标版本 --version/--help 和解析器证据登记命令、别名、选项和退出码；验证探测不执行 install/update/delete/start，尤其核实 cron add/create。
- [ ] 4.2 **H-304** 新增 `CliCommandsContractTest` 四场景并实现已验证有限管理选项/raw 入口；运行 `mvn -B -Dtest=CliCommandsContractTest test`，验证 raw 仍受资源和身份策略约束。
- [ ] 4.3 **H-304** 类型化机器用量与实际模型/时长信息；重跑同类测试，验证缺失保持缺失，不从自然语言日志推算 token 或账单。

## 5. CLI 批次发布与无污染验证

**原任务：** H-501, H-502, H-503, H-504。
**关联要求：** 本包全部要求及前置 sdk-compatibility 发布门禁。

**Files:**

- `docs/compatibility/cli-runtime-migration.md`
- `verification/cli-runtime/`

- [ ] 5.1 **H-501** 同步三分支并在对应真实 JDK 运行全部 CLI contract 与 `mvn -B --no-transfer-progress clean verify`；验证基础公开类型没有高版本依赖。
- [ ] 5.2 **H-502** 用伪 CLI 做长输出/慢消费/取消/超时故障测试，并在授权目标 CLI 验证机器流；记录内存、线程、进程与输出完整性，测试不得自动批准危险工具。
- [ ] 5.3 **H-503** 交付有限 execute→streaming→受管进程迁移说明；验证全局 Profile/用户配置未被测试改动，平台能力声明有证据。
- [ ] 5.4 **H-504** 汇总当前批次证据并复核前置契约，审查 diff/残留/迁移后批准候选版本；真实平台证据缺失时保持对应任务未完成。