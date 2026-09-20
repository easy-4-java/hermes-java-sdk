# ACP 权限请求与异步决定

## Purpose

为 ACP 服务端发起的权限挑战提供宿主可控的异步处理闭环，在没有授权、决定超时或对端退出时不自动放行，并保持协议读写持续运行和决定对当前会话挑战的严格关联。

## ADDED Requirements

### Requirement: AM-001 Deny by default and validate challenge scope

权限决定 MUST 由宿主显式提供并绑定当前连接、会话、挑战及工具调用的可用原生身份。无处理器、处理器失败、超时或挑战过期 MUST NOT 自动允许；MUST 使用协议可表达的拒绝/取消结果或明确终止该挑战。

#### Scenario: AM-001-S1 No permission handler
- **GIVEN** 工具请求权限但宿主未配置处理器
- **WHEN** 收到权限挑战
- **THEN** 返回协定拒绝/取消或明确错误，不默认批准该工具执行。

#### Scenario: AM-001-S2 Stale decision after reconnect
- **GIVEN** 宿主仍持有旧连接的权限决定
- **WHEN** 尝试用于新连接上的请求
- **THEN** 拒绝过期绑定，不能把旧授权扩展到新连接。

### Requirement: AM-002 Keep protocol reading live during asynchronous approval

权限等待 MUST 在不阻塞协议读循环的路径执行；并发请求和通知仍需可处理。一个挑战 MUST 至多产生一次有效应答；超时后的迟到决定、取消和进程退出 MUST 终止等待且不发送冲突应答。

#### Scenario: AM-002-S1 Bidirectional interleaving
- **GIVEN** Java 正等待 prompt，服务端反向请求权限
- **WHEN** 宿主异步查询状态后作出决定
- **THEN** 状态响应和通知能继续处理；权限应答可送达，形成真实双向闭环而非死锁。

#### Scenario: AM-002-S2 Late approval after timeout
- **GIVEN** 权限等待已经超时并发送拒绝
- **WHEN** 异步宿主稍后返回允许
- **THEN** 迟到允许不再发送，挑战只记录一个最终决定。