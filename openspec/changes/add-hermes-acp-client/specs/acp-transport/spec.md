# ACP 双向传输与请求关联

## Purpose

提供真正的双向 ACP 会话通道，而非仅启动一个命令进程；协商固定协议、区分请求响应和通知、限制挂起工作并在进程退出时结束所有等待，同时保留独立 stderr 诊断和明确的进程所有权。

## ADDED Requirements

### Requirement: AT-001 Initialize a versioned bidirectional protocol

SDK MUST 在发送会话操作前完成目标 ACP 协议初始化和能力协商；必须按选定协议的真实 framing 与消息结构读写，MUST NOT 假设等同于任意 CLI JSONL 或混入终端日志。版本不兼容 MUST 明确失败。

#### Scenario: AT-001-S1 Real bidirectional initialization
- **GIVEN** 启动支持选定 ACP 版本的进程
- **WHEN** Java 发送初始化并读取对端结果
- **THEN** 协商完成后才能发会话请求；stdout 仅作为协议通道，stderr 独立处理。

#### Scenario: AT-001-S2 Version mismatch
- **GIVEN** 对端不能接受 SDK 声明版本
- **WHEN** 收到初始化失败或不兼容应答
- **THEN** 初始化明确失败且清理拥有资源，不伪装为可用客户端。

### Requirement: AT-002 Correlate bounded concurrent requests and notifications

SDK MUST 区分 JSON-RPC 请求标识、会话标识及工具调用标识，关联乱序响应并独立处理通知。挂起请求、消息大小和等待时间 MUST 有界；未知响应 ID 或畸形消息 MUST 分类报告，不能误完成其他请求。

#### Scenario: AT-002-S1 Out of order replies with notifications
- **GIVEN** 并发发出两个请求，其间有会话通知
- **WHEN** 响应按相反顺序到达
- **THEN** 每个响应完成正确请求；通知交付独立监听者，不按到达顺序猜关联。

#### Scenario: AT-002-S2 Unmatched response ID
- **GIVEN** 收到不属于任何挂起请求的响应
- **WHEN** 分发该响应
- **THEN** 报告协议诊断并保留其他请求，不随意完成最早或同名请求。

### Requirement: AT-003 Drain pending work on exit without prompt replay

进程退出、传输关闭或初始化失败 MUST 使所有关联挂起请求以明确结果结束并回收拥有资源。重启 MUST NOT 自动重放此前 prompt；恢复只能使用经协商支持的会话恢复操作和宿主显式意图。

#### Scenario: AT-003-S1 Process exits with requests pending
- **GIVEN** 还有会话请求与权限应答未完成
- **WHEN** ACP 进程异常退出
- **THEN** 所有关联等待结束为进程/传输异常，无永久悬挂 Future。

#### Scenario: AT-003-S2 Process restarted
- **GIVEN** 上次 prompt 可能已部分执行
- **WHEN** 宿主重启进程
- **THEN** 不会自动重发 prompt；先初始化，仅按已支持能力显式恢复或重新创建。