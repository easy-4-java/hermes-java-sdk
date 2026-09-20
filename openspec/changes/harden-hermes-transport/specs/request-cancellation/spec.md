# 取消与资源所有权

## Purpose

明确本地等待、网络调用与远端运行之间的取消边界，要求 SDK 提供的公开取消入口真正收敛对应资源，并在共享传输、关闭竞态和结果未知条件下不影响宿主的其他工作。

## ADDED Requirements

### Requirement: RC-001 Propagate cancellation from public asynchronous results

SDK MUST 将其直接返回的可取消异步结果或显式取消令牌绑定到对应网络调用、未开始重试和排队任务；取消 MUST NOT 影响其他调用。对调用者自行派生的异步阶段 MUST 明确取消边界，并提供共享令牌作为可靠控制入口。

#### Scenario: RC-001-S1 Cancel returned future
- **GIVEN** 公开异步方法已发起尚未结束的网络调用
- **WHEN** 调用者取消 SDK 直接返回的 Future
- **THEN** 对应调用终止、重试停止且取消注册释放；并发其他请求继续运行。

#### Scenario: RC-001-S2 Cancel derived stage
- **GIVEN** 调用者从 SDK 结果自行派生新的 Future
- **WHEN** 仅取消该派生 Future 或改用共享令牌取消
- **THEN** 文档不承诺派生取消反向传播；使用共享令牌时对应网络调用确定被取消。

### Requirement: RC-002 Separate observation cancellation from remote stop

SDK MUST 将关闭观察、停止本地等待与请求远端停止作为不同操作。观察超时、取消 SSE 或关闭客户端 MUST NOT 隐式停止远端 Run。已经发送的写请求被取消时 MUST 保留结果可能未知的语义。

#### Scenario: RC-002-S1 Close observation only
- **GIVEN** 远端 Run 仍在运行
- **WHEN** 宿主关闭其事件观察句柄
- **THEN** 本地连接关闭且不发送远端 stop，运行状态不被伪造为 cancelled。

#### Scenario: RC-002-S2 Cancel after submission
- **GIVEN** 写请求已经发送
- **WHEN** 调用者取消网络请求
- **THEN** 网络资源收敛；结果可能未知被显式标记，允许按身份及操作键核对。

### Requirement: RC-003 Close only owned resources and handle races

SDK MUST 幂等关闭其拥有的订阅、执行器、连接及注册；MUST NOT 关闭外部共享传输的全局资源或无关调用。关闭与请求创建并发时 MUST 使新调用明确失败或被纳入关闭范围，不能留下不再可管理的资源。

#### Scenario: RC-003-S1 External shared client
- **GIVEN** SDK 与宿主无关任务使用同一外部传输
- **WHEN** 关闭 SDK 两次
- **THEN** SDK 的工作清理一次；宿主无关任务和外部全局资源仍可使用。

#### Scenario: RC-003-S2 Close during subscribe and retry
- **GIVEN** 一个线程关闭客户端，另一个创建订阅或安排重连
- **WHEN** 这两个动作并发执行
- **THEN** 创建操作被拒绝或随后被清理；无遗留重连、Future 或活动订阅。