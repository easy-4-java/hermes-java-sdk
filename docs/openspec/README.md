# Hermes Java SDK · OpenSpec 计划与规范

版本：V1.0；日期：2026-09-20；状态：**待实施的 OpenSpec 变更包**。

本文件是总入口。四个变更使用官方默认 `spec-driven` 产物组织，每个有 proposal、design、tasks、逐能力 delta spec 和逐场景测试蓝图。已有技术方案保留为背景，不直接改名冒充 spec。

**本次完成的是文档编写及有限的离线检查；不是 SDK 功能实现、真实联调或正式 OpenSpec CLI 校验通过。** 当前 CLI 下载受执行环境 DNS 限制，详细结果见 [验证报告](../../verification/openspec/REPORT.md)。没有远程提交，也没有在用户本机修改文件。

## 1. 交付清单

| Change | 能力域 | Requirement | Scenario | 待实施步骤 |
|---|---:|---:|---:|---:|
| `harden-hermes-transport` | 5 | 18 | 36 | 28 |
| `complete-hermes-http-runtime` | 6 | 18 | 36 | 25 |
| `enhance-hermes-cli-runtime` | 3 | 9 | 19 | 19 |
| `add-hermes-acp-client` | 3 | 8 | 16 | 20 |

本包共 17 个能力域、53 条 Requirement、107 个 Scenario、92 个待实施步骤。原方案的 28 个 H 任务和 28 个 T 验收编号全部保留映射，见 [traceability.md](traceability.md)。这些数字描述规划覆盖，不代表已实现/已测试的功能数量。

## 2. 实施顺序与阶段门禁

```text
harden-hermes-transport
        ↓ 正确性、身份与取消/所有权证据
complete-hermes-http-runtime
        ↓ Run/Responses/审批闭环与事实性观测
 enhance-hermes-cli-runtime
        ↓ 有界进程、JSONL 和平台回收
   add-hermes-acp-client
        ↓ 双向协议及权限互操作
```

这是项目级保守执行顺序，不是 OpenSpec CLI 自带的跨 change 调度。依赖也记录在 traceability.json；没有添加未经支持的 `.openspec.yaml` 依赖字段。四包可以同时评审，默认按上述顺序实施。CLI 的工程前置不要求运行时必须连接 HTTP。所有批次都执行三分支门禁，不把旧版本线测试拖到最后。

| 批次 | 必须先证明 | 不在该批做 |
|---|---|---|
| 传输加固 | 可信本地无需测试旁路；Profile 不串用；标准帧正确；断线不重复 POST；公开取消与资源收敛 | 大升级依赖、全仓拆模块、ACP/A2A |
| HTTP 闭环 | 提交→观察→审批→停止请求→真实终态；输出完整性与用量事实可解释 | 平台权限、账单定价、再实现 Agent Loop |
| CLI 增强 | 退出前可消费事件；stdout/stderr/队列有界；Profile 不污染；回收按平台验证 | 自动浏览器授权、TUI 模拟、完整 ACP |
| ACP | 真双向初始化/请求关联/权限应答；退出无悬挂；可选依赖不污染基础 SDK | 自动回退重做 prompt、宣称所有协议等价 |

## 3. 规范基线和 ADDED 语义

三个已观察 HEAD 均没有 openspec 根目录，见 [source-manifest.json](source-manifest.json)。因此这次建立的是新的规范基线，全部 delta 使用 `## ADDED Requirements`；不是宣称已有 HTTP/CLI 功能从未存在。提案中的 BREAKING 单独声明与现有代码的行为变化。

`openspec/specs/` 当前只保留 `.gitkeep`。未来行为只写入 `openspec/changes/`，不预先复制到已实施基线，不为了产物“完整”提前归档或勾选任务。

若把本包应用到更晚的分支且已经存在同名能力，先读取现有完整 spec 和 Scenario，转换对应 ADDED 为完整 MODIFIED 并重新校验；不要覆盖现有 config、AGENTS 或规范。

## 4. 怎样阅读与执行

先读该 change 的 proposal（为什么/范围），再读 specs（外部行为），再读 design（实现取舍、文件与接口），最后按 tasks 实施。`test-catalog.md` 将每个 Scenario 对应到拟新增测试文件、方法、输入和断言；不是已经运行的测试报告。

每个数字任务都带验证条件。新增 API/类/测试是设计目标，不是现有可直接编译调用的接口。按照红→绿回归与最小改动推进；提交前审查 diff，将共同逻辑与 JDK/JSON 适配分开。当前任务均未勾选，因为目标 Hermes 与实现验证尚未完成。

首批范围：原 H-001～H-004、H-101～H-106，以及当前包的 H-501～H-504 发布门禁。后续包里的 H-501～H-504 表示各批重复验收，不是要等到 ACP 才做基础检查。

## 5. 三分支要求

| 分支 | 已观察 HEAD | 要求维持的运行基线 |
|---|---|---|
| feature/1.0.x | 76710a9d7e72bd589d2244ecf9004a9bec3d3ed2 | Java 8 |
| feature/2.0.x | c97b68698751b2560b2472ee96874ccb10494e46 | Java 17 |
| feature/3.0.x | 3b3f148a09987442c71586cec809a62529f4c940 | Java 21 |

基线来自原方案与本轮 GitHub 观察；实际 JDK/POM/依赖和目标服务端由 H-001 复核。三线共享 wire 样本与业务语义，不强求 Jackson/OkHttp import 或历史构造器字节完全相同。新增基础公开 API 不强制 record/sealed/Flow/虚拟线程，不新增 Spring/Reactor 强依赖。

ACP 若不能满足某条线，隔离为可选适配范围并明确支持矩阵，不提高基础 SDK JDK。API Shape、Source Sync 和 fixtures hash 检查共同维护；仅在高 JDK 编译 --release 8 不足以证明 Java 8 运行支持。

## 6. 验证与状态

### 可直接运行的离线文档检查

在本包根目录或应用后的仓库根目录执行：

```bash
python3 scripts/check-openspec-package.py --root . --json
```

它检查结构、标识、GIVEN/WHEN/THEN、需求/任务/原编号映射、相对链接和无环依赖，不等同于官方解析器、归档合并或业务测试。 实施开始后可加 `--allow-progress` 允许已有真实证据支持的勾选；该工具针对本变更集，新增/归档变更后应维护对应索引，不把它当通用 OpenSpec 校验器。

### 官方 OpenSpec 检查

使用已安装的官方 CLI；本包记录的目标版本为 1.13.1，官方文档要求 Node >=20.19。没有把该 Node 要求施加到 Java SDK 的运行时。

```bash
npm install -g @fission-ai/openspec@1.13.1
bash scripts/validate-openspec-package.sh
```

脚本运行：

```bash
openspec --version
openspec validate --all --strict --no-interactive --json
openspec status --change harden-hermes-transport --json
openspec status --change complete-hermes-http-runtime --json
openspec status --change enhance-hermes-cli-runtime --json
openspec status --change add-hermes-acp-client --json
```

本次没有成功运行这些官方命令。脚本在没有 CLI 时会非零退出，输出不冒充通过；不会自动安装依赖，不归档变更，也不修改任务完成状态。产物状态完整只表示 proposal/specs/design/tasks 已写出，不表示实现完成。

### 代码与互操作验证

由实施任务在三条真实 JDK 上运行 `mvn -B --no-transfer-progress clean verify` 和指定 contract 类。再执行固定版本 Hermes 的真实互操作及平台资源测试；本包尚未执行这些测试。原 CI 的 Maven verify 入口可作为参考，不把其历史绿色结果当成本批代码证据。

## 7. 应用到仓库

本文件包是仅新增路径的文档 overlay：openspec/、docs/openspec/、docs/design/ 中的方案副本、新校验脚本、verification/openspec/。不包含 SDK 源码、POM、根 README 覆盖、用户密钥或 node_modules。

若使用随交付提供的 add-only patch，在目标 checkout 中先检查：

```bash
git status --short
git apply --check /path/to/hermes-java-sdk-openspec-v1.0.patch
git apply /path/to/hermes-java-sdk-openspec-v1.0.patch
python3 scripts/check-openspec-package.py --root . --json
```

`/path/to/` 仅是补丁位置提示，替换为实际保存路径。应用前检查分支 HEAD 和已有文件；发生同名文件或已建规范时先合并，不加 --force 覆盖。补丁只在空隔离目录检查过语法/可应用性，不宣称已在用户三条真实 checkout 中应用。

## 8. 归档与完成条件

顺序为 proposal/spec review → 实现与三线测试 → 真实互操作/资源/迁移证据 → 官方 strict validate → 代码和规格一致性评审 → 明确批准发布 → archive。归档前每个能力保留完整 Purpose；不提前制造“已实施”的根 spec。

发布不以任务数量或 Mock 绿色为依据。没有服务端版本、真实报文、JDK/OS 运行结果的支持声明必须留为未验证；详见 [协议采证门禁](protocol-evidence-gates.md)。

## 9. 非目标和平台边界

A2A、MCP Bridge、Dashboard/Admin、Browser Controller、Python Observer Bridge 另行立项。SDK 不直接依赖 Agent Fabric；平台适配器持久化审计、权限和费用规则。Agent-Job 与 Hermes Cron 同一任务只能有一个调度所有者，不能双方登记导致重复执行。工具事实、推理展示和最终答案不混淆，也不推断服务端未提供的内容。