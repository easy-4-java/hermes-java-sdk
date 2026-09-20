# CLI 结构化执行与进程管理变更提案

## Why

现有有限进程执行模型不能直接承担实时机器事件或长期交互；需要在保留有限入口的同时补齐有界输出、JSONL、进程控制和逐版本命令契约，避免内存无界与交互命令假支持。

## What Changes

- 保留有限 execute；增加结构化流和明确生命周期的受管进程入口。
- 固定每次执行的环境、Profile、工作目录和 argv，实施输出/并发/队列上限。
- 实现流式 UTF-8 JSONL、独立 stderr、初始化/终态/退出码核对。
- 支持可观察的取消和平台资源回收，未验证平台如实标记。
- 增加逐版本只读命令探测、类型化管理选项和机器用量。
- **BREAKING**：资源上限到达时明确失败；机器模式不再把部分输出或需要交互的流程当作完成。有限入口不悄悄变为交互会话。

## Capabilities

### New Capabilities

- `cli-execution`：CLI 执行上下文与有界资源。
- `cli-streaming`：CLI JSONL 与终态聚合。
- `cli-commands`：CLI 命令兼容与用量。

### Modified Capabilities

无。本次已核对的三条远程源码基线均没有 `openspec/` 目录，因此这里是新增规范能力，并不声称所有对应源码功能都是从零新增。待实施变更使用 ADDED；已存在的代码行为变化在上方显式说明。应用前若分支新增了规范，必须重新基准化并完整保留被修改要求的场景。

## Impact

HermesCli、HermesCliExecutor、Cli 配置、新增 cli.stream/cli.process、伪 CLI 和多平台测试；不改变全局 Profile。

实施前置：`harden-hermes-transport`、`complete-hermes-http-runtime`

依赖为本项目实施门禁，不是自定义 `.openspec.yaml` 字段。完整顺序见 [总计划](../../../docs/openspec/README.md)。本包尚未实施，tasks 保持未勾选，不能将产物齐全或格式有效当成代码完成。

审计和设计依据见 [来源记录](../../../docs/openspec/sources.md) 与 [原优化方案](../../../docs/design/hermes-java-sdk-optimization-plan-v1.0.md)。规范、设计和任务冲突时先修正文档；以 spec 的外部行为为实现验收基准，不用实现反向放宽规范。