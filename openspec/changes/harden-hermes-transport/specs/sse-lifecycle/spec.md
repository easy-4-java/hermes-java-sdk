# SSE 数据、终态与恢复

## Purpose

定义聊天、Responses、运行和会话事件流的原始帧保留、端点专用解释、终态与断线语义，在有界资源中保持事件顺序，并保证重新观察不会被误用为再次执行有副作用的 Agent 请求。

## ADDED Requirements

### Requirement: SE-001 Preserve the SSE frame before decoding payloads

SDK MUST 保留 SSE 事件名称、可用事件 ID 与原始 data 文本，再按目标端点解释业务载荷；MUST NOT 要求业务 JSON 额外包含名为 data 的嵌套字符串。没有服务端事件 ID 时 MUST NOT 伪造服务端 ID。

#### Scenario: SE-001-S1 Standard chat completion chunk
- **GIVEN** data 是包含 choices[0].delta.content 的合法聊天 JSON
- **WHEN** 消费含中文和空格的文本分片
- **THEN** 文本被正确读取且空格不丢失，原始 data 可访问，不需要额外 data 包装。

#### Scenario: SE-001-S2 Multi line and fragmented UTF8
- **GIVEN** SSE 使用 CRLF、多行 data、注释并在 UTF-8 字符中间分片
- **WHEN** 网络分批读取并完成事件
- **THEN** 仅完整帧交付解码，注释不成为业务事件，字符不损坏。

### Requirement: SE-002 Classify decoding and consumer failures separately

SDK MUST 区分协议解析、传输、消费者回调和 Agent 业务错误；未知非控制事件 MUST 保留可诊断信息。未识别的必需控制语义 MUST NOT 被视为已经处理。回调错误 MUST 被隔离或显式终止该订阅，且清理仍发生。

#### Scenario: SE-002-S1 Consumer throws
- **GIVEN** 业务 JSON 可成功解析
- **WHEN** 事件监听器抛出异常
- **THEN** 报告消费者错误而非 JSON 错误，并按配置终止或隔离订阅且释放对应资源。

#### Scenario: SE-002-S2 Unknown control or data event
- **GIVEN** 服务端发送客户端未识别的事件类型
- **WHEN** 客户端接收该事件
- **THEN** 保留原始类型与内容；普通未知事件可继续观察，未知必需控制事件不能被默认为批准或成功。

### Requirement: SE-003 Resolve terminal state at most once

SDK MUST 使用已验证的端点终态契约完成观察结果，至多完成一次；MUST 区分成功、失败、取消和不完整。EOF 本身 MUST NOT 被等同于成功；可核对运行状态时执行有界核对，否则报告流中断或结果未知并标记部分输出。

#### Scenario: SE-003-S1 Terminal then EOF
- **GIVEN** 服务端发送该端点的合法成功终态
- **WHEN** 随后 EOF 或重复终态到达
- **THEN** 成功只完成一次，资源回收，原始聊天 POST 次数保持一次。

#### Scenario: SE-003-S2 EOF without terminal
- **GIVEN** 已有文本增量但没有已验证终态
- **WHEN** 连接意外关闭
- **THEN** 结果标记不完整；无可核对句柄时返回中断或结果未知，不能以部分文本成功结束。

### Requirement: SE-004 Never blindly replay agent creation requests

SDK MUST NOT 因读取中断、凭据刷新或观察重连自动重发 Chat、Responses 或 Session Chat 的创建请求。写重试 MUST 同时具备已验证服务端幂等契约、同一操作键、同一载荷快照和同一身份范围。所有底层恢复策略 MUST 遵守该约束。

#### Scenario: SE-004-S1 Session POST disconnects after acceptance
- **GIVEN** 服务端已接受一轮会话聊天
- **WHEN** 响应流中途断开
- **THEN** 客户端不再次提交原始 POST；可用句柄仅用于观察或核对，外部工具不会因自动重放再执行。

#### Scenario: SE-004-S2 Write has no verified idempotency
- **GIVEN** 某写操作没有已验证幂等契约
- **WHEN** 发生可重试网络异常
- **THEN** 不重放该写请求；报告提交阶段和结果不确定性。

### Requirement: SE-005 Reattach with explicit continuity evidence

SDK MUST 将订阅既有运行与创建新运行分开；仅在目标服务支持时使用游标恢复。恢复 MUST 声明事件连续性是否得到证明；不能用相同文本内容去重，也不能把重新连接成功当成无损恢复。

#### Scenario: SE-005-S1 No replay cursor support
- **GIVEN** 已有运行可查询但服务端不提供事件恢复游标
- **WHEN** 客户端重新附着
- **THEN** 订阅和核对原运行，标记连续性未证明；不新建运行，不宣称所有中间事件已恢复。

#### Scenario: SE-005-S2 Repeated legitimate text
- **GIVEN** 连续两个事件有不同服务端身份但相同文本
- **WHEN** 消费重连前后的事件
- **THEN** 两个合法增量均保留；仅按已验证的事件身份/游标规则去重。

### Requirement: SE-006 Bound ordered event delivery and expose overflow

SDK MUST 为每个订阅按顺序派发事件且避免慢消费者阻塞无关请求；事件队列和重连调度 MUST 有界。默认溢出 MUST 显式失败，文本、审批、错误和终态 MUST NOT 静默丢弃。进度合并仅可由调用者显式启用。

#### Scenario: SE-006-S1 Queue saturated by slow consumer
- **GIVEN** 有界队列已满且后续到达审批或终态
- **WHEN** 客户端无法继续可靠交付
- **THEN** 明确报告溢出并关闭或核对观察，不丢控制事件后继续报告成功。

#### Scenario: SE-006-S2 One slow subscription
- **GIVEN** 订阅 A 的监听器阻塞而订阅 B 正常
- **WHEN** 两个订阅持续收到事件
- **THEN** A 的处理不阻塞 B 的传输，单个订阅内部顺序保持。