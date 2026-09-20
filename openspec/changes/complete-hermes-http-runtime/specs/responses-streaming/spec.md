# Responses 结构化流

## Purpose

让 Java 调用者通过明确的流式入口消费 Responses 事件并获得可解释的最终结果，按服务端输出项关联内容，避免工具活动、过程信息与最终答案混合，也避免中断与重复终态造成假成功或重复累计。

## ADDED Requirements

### Requirement: RS-001 Separate streaming and ordinary JSON APIs

SDK MUST 提供独立的 Responses 流式入口；普通 JSON 响应入口收到 stream=true 时 MUST 在发送前拒绝并说明正确入口。流式入口 MUST 复用安全、身份、取消与 SSE 终态规则，而不是等待完整正文再执行 JSON 解析。

#### Scenario: RS-001-S1 Streaming request sent to JSON API
- **GIVEN** 调用者在普通 Responses 方法中开启 stream
- **WHEN** 准备发送请求
- **THEN** 请求未发送即报告参数与入口不匹配，并指引专用流式入口。

#### Scenario: RS-001-S2 Stream opens
- **GIVEN** 目标服务支持 Responses 流
- **WHEN** 通过专用入口提交
- **THEN** 进程或 HTTP 调用未结束前即可观察事件，并取得独立取消/观察句柄。

### Requirement: RS-002 Accumulate indexed outputs without reexecuting tools

SDK MUST 按原生输出项标识、索引及内容位置聚合事件；最终答案、过程消息和工具活动 MUST 分离。重复终态不得重复累计。服务端已完成的工具活动 MUST NOT 被当成要求 Java 自动执行该工具的命令。

#### Scenario: RS-002-S1 Interleaved outputs
- **GIVEN** 两个输出项交错到达文本和工具活动
- **WHEN** 累积器生成结果
- **THEN** 每个内容归入正确项，工具记录独立保留，最终文本不混入过程日志。

#### Scenario: RS-002-S2 Terminal repeats prior content
- **GIVEN** 最后一条权威结果包含此前已发出的增量文本
- **WHEN** 终态及重复终态到达
- **THEN** 最终结果不重复拼接，完成一次且没有客户端二次工具执行。

### Requirement: RS-003 Expose complete versus partial results

SDK MUST 以端点可验证终态决定 Responses 完整性；空文本但合法终态 MUST 可成功。缺终态、无法恢复的事件缺口或解析失败 MUST 明确标记不完整，并保留可安全访问的部分输出和原因。

#### Scenario: RS-003-S1 Legitimate empty output
- **GIVEN** 服务端返回合法成功终态且无文本
- **WHEN** 构建最终结果
- **THEN** 成功状态成立，空输出不被误判为网络错误。

#### Scenario: RS-003-S2 Output gap after disconnect
- **GIVEN** 已接收若干输出项但连续性无法证明
- **WHEN** 连接恢复后获取运行最终状态
- **THEN** 运行终态与观察完整性分别报告，不宣称此前流已无损恢复。