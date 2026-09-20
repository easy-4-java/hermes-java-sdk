# OpenSpec 计划包 · 校验与交付记录

记录日期：2026-09-20。对象：本次生成的 Hermes Java SDK OpenSpec 文档 overlay。

**结论：已完成四个变更包和有限离线文档检查；官方 OpenSpec CLI 校验未运行，SDK 编译与 Hermes 互操作未运行。**

## 1. 已生成的规划产物

| 统计 | 数量 | 含义 |
|---|---:|---|
| Change | 4 | 四批独立评审和分阶段实施的变更 |
| Capability | 17 | 新建的规范能力域 |
| Requirement | 53 | 以 MUST/MUST NOT 表达的外部行为要求 |
| Scenario | 107 | GIVEN/WHEN/THEN 验收场景，不是实测通过数 |
| 实施任务步骤 | 92 | 全部未勾选 |
| 原 H 任务映射 | 28/28 | 原优化方案任务未遗漏 |
| 原 T 验收映射 | 28/28 | 原验收编号均可追踪到具体 Scenario |

每包包含 `.openspec.yaml`、proposal.md、design.md、tasks.md、test-catalog.md、specs/<capability>/spec.md。配置为 `schema: spec-driven`。根 openspec/specs 仅有占位 .gitkeep，未将计划行为提前归档为已实施规范。

## 2. 本轮实际执行的检查

| 检查 | 实际结果 | 证据与边界 |
|---|---|---|
| 自有离线文档检查 | 通过，0 项错误 | [offline-check.json](offline-check.json)；只检查已说明的结构、编号、映射、路径和依赖 |
| 离线检查器回归 | 7/7 通过 | [checker-tests.log](checker-tests.log)；测试检查器，不是 SDK |
| YAML 语法与约定字段类型 | 5/5 可解析 | [yaml-syntax-check.json](yaml-syntax-check.json)；PyYAML，不是官方 schema 校验 |
| 官方 CLI 包装脚本语法 | exit 0 | [script-syntax-check.json](script-syntax-check.json)；仅 bash 语法 |
| 官方 CLI 可用性入口 | exit 2，NOT_RUN | [official-cli-availability.log](official-cli-availability.log)；未安装官方 openspec |
| add-only 补丁 | 检查、应用与逐文件字节比对通过 | [patch-check.json](patch-check.json)；仅空隔离 Git 目录，不是用户真实 checkout |

实际使用的工具版本见 [environment.json](environment.json)。文件完整性清单见 [files.sha256](files.sha256)；该清单不包含自身。

### 离线检查器具体检查

检查四个已知变更和产物是否齐全、capability 清单是否一致、Purpose 是否实质填写、ADDED/Requirement/Scenario 结构、规范关键词、唯一编号、每个场景 GIVEN/WHEN/THEN、任务编号和验证描述、每个 Requirement 的任务与测试目录归属、原 H/T 编号覆盖、相对链接以及依赖无环。

回归检查包括：正常包；缺失 THEN；重复 Requirement ID；Requirement 无实施任务；计划任务提前勾选；变更依赖成环；相对文件链接失效。没有用“检查器能通过”替代人工语义审阅。

本工具不实现完整 YAML/OpenSpec schema，不模拟 archive 合并，不验证 Markdown anchor，不验证服务端 wire shape，不证明 SDK 行为或三 JDK 兼容。

## 3. 官方 OpenSpec 校验为何未完成

本环境没有已安装的官方 `openspec`。尝试查询 npm registry 返回 EAI_AGAIN（无法解析 registry.npmjs.org），因此无法安装所记录的目标 CLI 版本 1.13.1。版本目标来自官方仓库 package.json，不表示本机已经运行该版本。

包装脚本被实际调用，按设计在未找到 CLI 时返回 2，并明确输出 NOT_RUN。未伪造 validate/status 的 JSON 或成功日志，也没有用自有校验器冒充官方工具。

在安装官方 CLI 的环境中，由宿主运行：

```bash
bash scripts/validate-openspec-package.sh
```

脚本记录 `openspec --version`、`openspec validate --all --strict --no-interactive --json` 和四包 `openspec status --change ... --json` 的退出码、stdout/stderr。失败即停止，不自动安装、不改 SDK、不勾任务、不归档。即便 status 完整，也只代表文档产物完整。

## 4. 明确未执行的内容

没有修改 SDK Java 源码、POM、根 README；没有在用户本机 clone 或应用文件；没有对 GitHub 写入、提交、推送或创建 PR；没有运行 Maven/JDK8/17/21 构建、SDK 单元测试、真实 Hermes 互操作、真实进程压力测试、依赖漏洞扫描或生产就绪检查。

任务中的测试类、测试方法、协议捕获和 verification 路径是未来实施目标，不是已有结果。实际部署的 Hermes 版本仍由第一批采证任务固定，未知 wire 能力不得被实现者填造。

## 5. 完成与迁移边界

规范编写阶段已经完成；官方 CLI 严格校验仍是进入实现/归档流程前的待执行检查。实现必须逐任务保留回归与三分支证据。应用补丁前先检查目标分支与同名文件，已有 OpenSpec 时重新对齐规范，不强制覆盖。

补丁和 ZIP 的最终完整性校验随交付提供。根 openspec/specs 未发生已实施规范变更，92 个实现步骤保持未完成。