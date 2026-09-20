# Agent Run 句柄与状态核对

## Purpose

为提交、重新附着、停止和等待 Agent 运行提供持续可观察的生命周期契约，严格分离本地连接状态和服务端执行状态，并利用已确认的幂等语义处理响应丢失与进程恢复而不重复创建工作。

## ADDED Requirements

### Requirement: RN-001 Submit and attach expose the same lifecycle contract

SDK MUST 为新提交运行和既有运行附着提供等价的状态查询、事件观察、停止请求及终态等待能力。附着 MUST NOT 创建新运行，身份与可见性 MUST 沿用已绑定上下文。

#### Scenario: RN-001-S1 Attach an existing run
- **GIVEN** 宿主持有原运行标识及授权身份
- **WHEN** 重新创建观察句柄
- **THEN** 只查询/订阅该运行，创建 Run 的计数不增加。

#### Scenario: RN-001-S2 Run inaccessible
- **GIVEN** 绑定身份无法读取某运行
- **WHEN** 查询返回不存在或无权限
- **THEN** 暴露对应错误，不推断其他 Profile 是否拥有该运行，也不报告成功。

### Requirement: RN-002 Separate local observation and server state

SDK MUST 分别表达服务端状态与本地观察状态；未知原生状态 MUST 原样保留。观察等待 MUST 支持有界截止时间，超时或断线 MUST NOT 被映射为服务端已取消、已失败或已成功。

#### Scenario: RN-002-S1 Observation times out
- **GIVEN** 运行仍在执行
- **WHEN** 本地等待达到截止时间
- **THEN** 返回观察超时，不调用远端停止，不改变服务端状态；之后仍可重新观察。

#### Scenario: RN-002-S2 Unknown server status
- **GIVEN** 服务端返回尚未识别的状态字符串
- **WHEN** SDK 更新快照
- **THEN** 保留原始状态并标记未知，不擅自选择任何终态。

### Requirement: RN-003 Keep one idempotent operation identity across retries

创建运行的自动重试 MUST 以服务端已验证幂等契约为前提，复用同一键、请求快照与身份范围。键与载荷冲突 MUST 返回冲突，不换新键重试。跨应用重启的恢复 MUST 由宿主持久化操作身份，SDK MUST NOT 承诺仅靠内存实现 exactly-once。

#### Scenario: RN-003-S1 Submission accepted but response lost
- **GIVEN** 服务端支持幂等且本次键 K 已接受
- **WHEN** 响应丢失后执行允许的重试
- **THEN** 继续使用 K 和同一载荷/身份，核对原操作，不创建第二个业务运行。

#### Scenario: RN-003-S2 Same key different payload
- **GIVEN** 宿主复用键 K 但传入另一载荷
- **WHEN** 服务端或客户端检测冲突
- **THEN** 返回幂等冲突，绝不自动生成替代键规避冲突。

### Requirement: RN-004 Distinguish stop acknowledgement from confirmed terminal

SDK MUST 将停止请求的接收结果与运行的实际终态分开。停止应答后 MUST 通过事件或有界查询确认终态；停止与自然完成竞态 MUST 保留实际终态，不伪造 worker 已退出或 cancelled。

#### Scenario: RN-004-S1 Stop accepted while worker runs
- **GIVEN** 停止接口返回已接收
- **WHEN** 运行仍报告正在停止
- **THEN** 句柄继续观察至真实终态，不提前宣布已停止。

#### Scenario: RN-004-S2 Completion races stop
- **GIVEN** 运行在停止提交前后自然完成
- **WHEN** 服务端确认最终完成
- **THEN** 报告实际完成状态和停止请求结果，不强制改写为取消。