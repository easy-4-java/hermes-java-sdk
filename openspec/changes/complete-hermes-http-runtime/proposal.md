# HTTP 运行闭环变更提案

## Why

在传输正确性基础上，将分散的 HTTP 方法补成从提交到观察、审批、停止和真实终态核对的完整运行契约。解决类型化结果、请求上下文、能力发现和事实性观测不足，不能用新增方法数量代替互操作证明。

## What Changes

- 统一请求上下文、单次选项和请求级模型选择，保留实际运行模型。
- 新增 Responses 专用流、输出项聚合及完整性标记。
- 新增 Run 句柄、幂等提交、附着、状态核对和真实终态等待。
- 新增默认不批准的异步审批闭环。
- 类型化模型元数据、会话、Jobs、能力与健康访问，保留 raw 入口。
- 提供统一错误、响应元数据、用量事实与可选观测。
- **BREAKING**：普通 JSON 入口拒绝 stream=true；高级入口不再允许绕过身份；新类型化删除接口不把权限/服务错误折叠成 false。旧 boolean 方法保留单独迁移策略。

## Capabilities

### New Capabilities

- `request-context`：请求上下文与模型选择。
- `responses-streaming`：Responses 结构化流。
- `run-lifecycle`：Agent Run 句柄与状态核对。
- `approval-workflow`：人工审批闭环。
- `runtime-resources`：模型、会话、Jobs 与能力发现。
- `runtime-observability`：错误、运行事实与安全观测。

### Modified Capabilities

无。本次已核对的三条远程源码基线均没有 `openspec/` 目录，因此这里是新增规范能力，并不声称所有对应源码功能都是从零新增。待实施变更使用 ADDED；已存在的代码行为变化在上方显式说明。应用前若分支新增了规范，必须重新基准化并完整保留被修改要求的场景。

## Impact

HermesClient 兼容委托、请求/响应模型、HTTP/SSE 解码、runtime 与 observability 包、契约测试及迁移文档。

实施前置：`harden-hermes-transport`

依赖为本项目实施门禁，不是自定义 `.openspec.yaml` 字段。完整顺序见 [总计划](../../../docs/openspec/README.md)。本包尚未实施，tasks 保持未勾选，不能将产物齐全或格式有效当成代码完成。

审计和设计依据见 [来源记录](../../../docs/openspec/sources.md) 与 [原优化方案](../../../docs/design/hermes-java-sdk-optimization-plan-v1.0.md)。规范、设计和任务冲突时先修正文档；以 spec 的外部行为为实现验收基准，不用实现反向放宽规范。