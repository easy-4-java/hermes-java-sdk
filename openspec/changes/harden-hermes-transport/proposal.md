# 传输、安全与兼容性加固变更提案

## Why

前序方案指出可信本地端点、Profile 身份、SSE 载荷解释、终态/重放、公开取消链和资源所有权存在需要回归验证的风险。先建立可验证传输底座，避免在错误语义上继续叠加更多 API；静态风险不等同于已发生线上事故。

## What Changes

- 新增正式可信本地/私网/公网端点策略，绑定实际连接地址和鉴权边界。
- 新增命名 Profile 凭据解析、轮换与身份相关缓存隔离。
- 将 SSE 原始帧与业务解码分开，严格终态、有界有序派发和可解释的连续性。
- 修复或通过公开 API 回归验证取消传播、关闭竞态及外部资源所有权。
- 建立三分支契约样本、真实 JDK、迁移与发布证据门禁。
- **BREAKING**：默认不再盲目重放聊天 POST；缺少命名 Profile 凭据明确失败；队列溢出/未知终态不再静默当成功。原测试旁路只弃用，不在修复版直接删签名。

## Capabilities

### New Capabilities

- `trusted-endpoints`：可信端点与出站策略。
- `profile-authentication`：Profile 身份与凭据隔离。
- `sse-lifecycle`：SSE 数据、终态与恢复。
- `request-cancellation`：取消与资源所有权。
- `sdk-compatibility`：三版本线与证据化发布。

### Modified Capabilities

无。本次已核对的三条远程源码基线均没有 `openspec/` 目录，因此这里是新增规范能力，并不声称所有对应源码功能都是从零新增。待实施变更使用 ADDED；已存在的代码行为变化在上方显式说明。应用前若分支新增了规范，必须重新基准化并完整保留被修改要求的场景。

## Impact

HermesHttpClientConfig、EndpointGuard、HermesClient、HTTP/SSE 客户端、传输工厂、测试与三分支 CI；不强制依赖升级。

实施前置：无前置变更；先完成本包的源码和服务端契约采证。

依赖为本项目实施门禁，不是自定义 `.openspec.yaml` 字段。完整顺序见 [总计划](../../../docs/openspec/README.md)。本包尚未实施，tasks 保持未勾选，不能将产物齐全或格式有效当成代码完成。

审计和设计依据见 [来源记录](../../../docs/openspec/sources.md) 与 [原优化方案](../../../docs/design/hermes-java-sdk-optimization-plan-v1.0.md)。规范、设计和任务冲突时先修正文档；以 spec 的外部行为为实现验收基准，不用实现反向放宽规范。