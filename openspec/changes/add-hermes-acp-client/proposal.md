# ACP 双向协议客户端变更提案

## Why

调用 hermes acp 命令并等待退出不等于实现双向 ACP。需要持续传输、请求关联、会话及权限回调，使 Java 宿主真正完成交互工作，同时不强迫基础 SDK 用户安装或加载可选协议。

## What Changes

- 实现初始化与版本协商、持续双向协议、请求 ID 关联和有界 pending。
- 实现会话创建、prompt、通知、取消与按协商能力可用的扩展操作。
- 实现默认拒绝的异步权限回调，防止协议读循环死锁。
- 实现异常退出的挂起请求结束和可控进程回收。
- ACP 显式启用；协议依赖若超出核心 JDK 基线则隔离在可选分发边界。
- 不自动跨协议重做 prompt，不宣称 HTTP 与 ACP 工具/媒体/恢复能力等价。

## Capabilities

### New Capabilities

- `acp-transport`：ACP 双向传输与请求关联。
- `acp-sessions`：ACP 会话与能力边界。
- `acp-permissions`：ACP 权限请求与异步决定。

### Modified Capabilities

无。本次已核对的三条远程源码基线均没有 `openspec/` 目录，因此这里是新增规范能力，并不声称所有对应源码功能都是从零新增。待实施变更使用 ADDED；已存在的代码行为变化在上方显式说明。应用前若分支新增了规范，必须重新基准化并完整保留被修改要求的场景。

## Impact

新增可选 ACP 逻辑层、JSON-RPC 分发、权限/会话适配和互操作测试；基础 HTTP/CLI 无需启用 ACP。

实施前置：`enhance-hermes-cli-runtime`

依赖为本项目实施门禁，不是自定义 `.openspec.yaml` 字段。完整顺序见 [总计划](../../../docs/openspec/README.md)。本包尚未实施，tasks 保持未勾选，不能将产物齐全或格式有效当成代码完成。

审计和设计依据见 [来源记录](../../../docs/openspec/sources.md) 与 [原优化方案](../../../docs/design/hermes-java-sdk-optimization-plan-v1.0.md)。规范、设计和任务冲突时先修正文档；以 spec 的外部行为为实现验收基准，不用实现反向放宽规范。