# 请求上下文与模型选择

## Purpose

为 HTTP 各领域请求提供明确的身份、会话、模型选择与调用选项合并规则，使客户端默认值和单次覆盖保持可预测，同时避免可变对象、原始请求头和协议扩展破坏既定身份隔离。

## ADDED Requirements

### Requirement: CT-001 Snapshot context and preserve identity boundaries

SDK MUST 在发送前固定本次身份、会话、调用选项和业务载荷的有效快照；本次显式业务值优先于客户端默认值。会话标识与长期记忆范围 MUST 分别携带。SDK MUST NOT 从任意用户请求头推断或授予租户权限。

#### Scenario: CT-001-S1 Concurrent mutation
- **GIVEN** 同一个请求构建对象将在发送后被另一线程修改
- **WHEN** SDK 创建本次传输请求
- **THEN** 已发送操作继续使用固定快照，之后的修改只影响后续明确创建的操作。

#### Scenario: CT-001-S2 Separate session and memory keys
- **GIVEN** 一次请求指定不同的会话 ID 和记忆范围键
- **WHEN** 创建 Chat、Run 或会话操作
- **THEN** 两种标识按各自契约传输，不互相覆盖，也不将其等同于平台授权。

### Requirement: CT-002 Propagate verified model options and preserve actual runtime

SDK MUST 将本次指定的 model、provider 及 model options 按目标端点已验证契约传输；MUST 保留服务端返回的实际运行模型与请求模型之间的区别。不存在的字段 MUST 保持缺失，不能伪造实际模型或保证服务端一定采用请求值。

#### Scenario: CT-002-S1 Explicit option overrides default
- **GIVEN** 客户端有默认模型和 provider
- **WHEN** 请求显式指定另一组有效选择
- **THEN** 服务端收到本次显式字段；未指定的可用默认值按约定补齐。

#### Scenario: CT-002-S2 Server selects a fallback
- **GIVEN** 请求模型 A，响应声明实际模型 B
- **WHEN** SDK 构造运行结果
- **THEN** 同时保留请求选择和实际 B，不将 A 写成实际执行模型。

### Requirement: CT-003 Validate media and raw extensions against capabilities

SDK MUST 为目标端点已确认支持的结构化输入提供可验证的表达；MUST NOT 将聊天兼容性当作完整媒体或文件 API 支持。原始扩展入口 MUST 保留安全、身份、资源限制和未知能力处理，不能成为绕过策略的入口。

#### Scenario: CT-003-S1 Unsupported media type
- **GIVEN** 当前端点未确认支持某媒体输入
- **WHEN** 高层请求使用该输入
- **THEN** 返回不支持或能力未知，不静默丢字段后改成文本请求。

#### Scenario: CT-003-S2 Raw extension with identity conflict
- **GIVEN** 调用者使用高级扩展字段或请求头
- **WHEN** 扩展试图改变 Profile 或认证
- **THEN** 安全检查仍拒绝冲突；普通合法扩展被保留。