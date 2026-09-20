# CLI JSONL 与终态聚合

## Purpose

使应用在 Hermes 单轮 CLI 进程退出前消费机器可读事件，正确处理流式 UTF-8、记录边界与独立 stderr，并通过终态记录和退出结果共同判断成功、启动失败及不完整执行。

## ADDED Requirements

### Requirement: CS-001 Decode machine output incrementally and preserve data

SDK MUST 按已固定 CLI 契约增量解析机器输出，分离 stdout 与 stderr；MUST 处理跨读边界 UTF-8 与半行，禁止对文本增量 trim。未知事件 MUST 保留，畸形或超限记录 MUST 明确分类，不能混入自然语言终端日志解析。

#### Scenario: CS-001-S1 UTF8 record fragmented
- **GIVEN** 一条中文 JSONL 记录分多次读取且字符字节被拆开
- **WHEN** 读取至完整换行
- **THEN** 恰好生成一个正确事件，前后合法空格保持。

#### Scenario: CS-001-S2 Mixed stderr diagnostics
- **GIVEN** stdout 提供有效机器事件而 stderr 提供诊断
- **WHEN** 两路数据交错到达
- **THEN** 诊断不进入事件 JSON 解析，两个通道独立且均受限。

### Requirement: CS-002 Reconcile protocol terminal with process exit

SDK MUST 同时记录协议初始化、终态结果与进程退出。初始化前失败 MUST 返回启动/配置/认证等可识别错误；已开始而无合法终态 MUST 报告协议中断或结果未知。非成功退出与声称成功终态冲突时 MUST NOT 无条件返回成功。

#### Scenario: CS-002-S1 Failure before initialization
- **GIVEN** 程序不存在或在协议初始化前退出
- **WHEN** 执行结束
- **THEN** 返回启动失败、可用退出信息和受限 stderr，不一律报缺少 result。

#### Scenario: CS-002-S2 Output without terminal
- **GIVEN** 进程已发送文本事件但没有合法终态
- **WHEN** 进程退出
- **THEN** 部分输出可访问但最终结果不完整，不按文本非空判断成功。

#### Scenario: CS-002-S3 Terminal conflicts with exit
- **GIVEN** 收到成功终态后进程非成功退出
- **WHEN** 生成最终执行结果
- **THEN** 保留终态与退出两份证据并报告冲突，不能伪造完整成功。

### Requirement: CS-003 Deliver live events and avoid duplicate final text

流式入口 MUST 在进程退出前交付完整事件并提供取消与最终结果句柄；最终权威结果与增量 MUST 使用明确的聚合规则，不能重复拼接。生命周期和用量事实 MUST 遵循统一观测中的缺失值、隐私与去重规则。

#### Scenario: CS-003-S1 Progress before process exit
- **GIVEN** 测试 CLI 发出事件后延迟退出
- **WHEN** 监听者等待首个事件
- **THEN** 首事件在进程结束前被观察到，有限执行的异步包装不能冒充此能力。

#### Scenario: CS-003-S2 Final result repeats deltas
- **GIVEN** 最后结果文本包含已流式显示的内容
- **WHEN** 执行结果聚合
- **THEN** 最终文本只保留一次，重复终态不重复计入用量。