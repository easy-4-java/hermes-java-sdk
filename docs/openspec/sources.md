# 来源、版本与证据边界

## 已读取资料

本包转换自用户会话中的《Hermes Java SDK 优化与能力补齐方案 V1.0》，原文保留在 [docs/design](../design/hermes-java-sdk-optimization-plan-v1.0.md)，SHA-256 见 [source-manifest.json](source-manifest.json)。该方案含 28 个 H 任务和 28 个 T 验收编号。

本轮通过 GitHub 连接读取分支集合及三个指定 HEAD 的完整根 tree；它们均没有 openspec 根目录。树响应是完整根目录，并非用搜索无结果推断不存在。源码细节风险继承原方案的静态审计，本轮没有重新执行 SDK、codegraph 或真实 Hermes。

| 资料 | 用途与边界 |
|---|---|
| [三分支观察清单](source-manifest.json) | 固定本次写作对象，不代表用户本机已 clone |
| [OpenSpec 默认 schema](https://raw.githubusercontent.com/Fission-AI/OpenSpec/main/schemas/spec-driven/schema.yaml) | proposal/specs/design/tasks，新增能力 Purpose 与 ADDED，要求和场景标题 |
| [OpenSpec spec 模板](https://raw.githubusercontent.com/Fission-AI/OpenSpec/main/schemas/spec-driven/templates/spec.md) | 四级 Scenario、规范强制词、可验证条件 |
| [OpenSpec CLI 文档](https://openspec.dev/docs/cli) | validate/status、结构检查与归档边界 |
| [OpenSpec package 描述](https://raw.githubusercontent.com/Fission-AI/OpenSpec/main/package.json) | 本轮观察版本 1.13.1、Node >=20.19；npm 包本身未能下载验证 |
| [OpenSpec 配置说明](https://github.com/Fission-AI/OpenSpec/blob/main/docs/opsx.md) | config.yaml 中 schema/context/rules |

这些官方文件读取于 2026-09-20，main 文档会变；运行正式 CLI 时以实际安装版本为准并记录版本。我们没有 vendoring 或改写 OpenSpec CLI，没有把自写检查器伪装为官方实现。

## Hermes 依据的使用方式

原方案附录中 C0～C5 为固定 SDK commit 源码，S1～S7 为 Hermes 官方 API/CLI/ACP/Cron/A2A/MCP/Profile 文档。用户给定全文入口为 <https://hermes-agent.nousresearch.com/docs/llms-full.txt>。

本轮任务是将已形成方案转成行为规范，不额外宣称这些文档中的能力已经在目标服务部署、抓包或联调。任何 wire 字段、CLI 别名、ACP framing 或服务端终态须由实施前置任务固定。文档示例、人工样本、真实捕获在 fixtures manifest 中必须区分。

## 当前验证边界

离线检查只覆盖文档结构、编号、场景字段、任务/旧编号映射、相对链接、依赖无环与清单一致性；不执行 OpenSpec 官方解析器/归档合并，也不验证业务代码。官方 CLI 未安装；npm 访问 registry.npmjs.org 返回 EAI_AGAIN。此限制和实际离线检查结果记录在 verification/openspec/。

本包没有提交 GitHub、没有改 SDK 源码、没有创建或归档已实现 spec。只在本次会话沙箱生成文档与校验工具。