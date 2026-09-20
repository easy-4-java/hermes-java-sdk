# 错误、运行事实与安全观测

## Purpose

提供可分类错误、必要响应元数据、事实性模型和用量以及受控生命周期观测，使宿主能够审计和诊断；正文、凭据与原始协议信息不会因为调试或遥测自动外送，费用与授权仍由宿主负责。

## ADDED Requirements

### Requirement: OB-001 Preserve actionable errors and response metadata

SDK MUST 保留 HTTP 状态、可用服务端错误码、必要响应头和受限诊断正文；错误 MUST 区分配置、认证、权限、限流、幂等冲突、解析、流中断、审批、进程、远端业务及结果未知。删除等新接口 MUST 区分成功、不存在、无权限与服务错误。

#### Scenario: OB-001-S1 Delete returns forbidden
- **GIVEN** 删除操作被服务端拒绝
- **WHEN** 调用类型化删除入口
- **THEN** 返回权限错误而不是 false 或成功，保留受限诊断与关联信息。

#### Scenario: OB-001-S2 Retry after exceeds deadline
- **GIVEN** 限流应答给出 Retry-After
- **WHEN** 所需等待超过本次截止时间
- **THEN** 返回限流/截止结果，不超期等待或重放无幂等保障的写请求。

### Requirement: OB-002 Expose factual usage without double accounting

SDK MUST 保留服务端实际运行模型、可用 token 及缓存用量与来源；缺失值 MUST 保持缺失。重复终态或重附着 MUST NOT 在 SDK 的最终用量聚合中重复累加。费用估算 MUST 与真实账单区分且交由宿主定价逻辑处理。

#### Scenario: OB-002-S1 Missing usage
- **GIVEN** 运行结果不含缓存 token 或费用
- **WHEN** 构造用量结果
- **THEN** 字段保持未提供，不用零或猜测值冒充服务端事实。

#### Scenario: OB-002-S2 Duplicate terminal usage
- **GIVEN** 相同运行终态携带相同用量重复到达
- **WHEN** 句柄完成并发布最终结果
- **THEN** 最终用量不倍增；跨进程结算仍要求宿主稳定身份防重。

### Requirement: OB-003 Bound and redact observability without mandatory backends

生命周期观测 MUST 可选且不强制遥测后端；默认日志 MUST 不记录请求/响应正文、秘密头、Cookie、完整环境及未授权原始报文。调试正文 MUST 有长度限制和脱敏。指标 MUST 避免无限高基数身份标签；原始载荷的内存可访问性不构成落盘授权。

#### Scenario: OB-003-S1 Sensitive data in diagnostics
- **GIVEN** 错误与环境包含测试密钥、Cookie 和用户正文
- **WHEN** 默认日志及主动 BODY 调试分别记录
- **THEN** 默认不记录正文；敏感字段均脱敏；开启正文仍限长，原始数据不自动外送。

#### Scenario: OB-003-S2 No telemetry configured
- **GIVEN** 应用没有安装观测后端
- **WHEN** 执行 HTTP、CLI 或 ACP 工作
- **THEN** 核心行为仍正常；观察者失败被隔离而不阻塞关键资源清理。