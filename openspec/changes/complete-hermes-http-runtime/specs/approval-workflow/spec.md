# 人工审批闭环

## Purpose

定义服务端审批请求与宿主决策之间的身份绑定、超时与失败语义，确保任何缺失、过期或异常均不会自动扩大权限；提交审批仅改变当前挑战处理，不等同于运行完成。

## ADDED Requirements

### Requirement: AP-001 Default to no approval and bind decisions

SDK MUST 把审批决定绑定到端点、Profile、运行和当前挑战；服务端提供挑战 ID、版本或有效期时 MUST 利用它们，缺失时不得伪造。无处理器、处理器异常或超时 MUST NOT 自动批准，高权限长期允许 MUST 由宿主明确授权。

#### Scenario: AP-001-S1 Handler missing or fails
- **GIVEN** 收到审批请求但无处理器或处理器抛错
- **WHEN** SDK 处理挑战
- **THEN** 报告审批未完成/拒绝或按已确认契约拒绝，不自动发送允许。

#### Scenario: AP-001-S2 Decision crosses run boundary
- **GIVEN** 决定来自运行 A 的挑战
- **WHEN** 被用于运行 B 或其他 Profile
- **THEN** SDK 拒绝提交，不把它降级成不带绑定的通用决定。

### Requirement: AP-002 Process asynchronous decisions without claiming exactly once

审批等待 MUST 不阻塞协议事件读取；超时、过期、断连与重复提交 MUST 有明确结果。本地防重 MUST NOT 被表述为服务端 exactly-once。审批接收成功 MUST NOT 被表述为 Agent 运行成功。

#### Scenario: AP-002-S1 Approval waits while events arrive
- **GIVEN** 宿主异步处理审批
- **WHEN** 服务端继续发送控制和状态事件
- **THEN** 读循环继续处理事件；审批完成后仅更新挑战处理，运行仍等待其真实终态。

#### Scenario: AP-002-S2 Duplicate or expired decision
- **GIVEN** 同一决定重复提交或挑战已过期
- **WHEN** SDK 或服务端返回冲突/过期
- **THEN** 明确报告拒绝、冲突或结果未知，不自动扩大授权或再提交新挑战。