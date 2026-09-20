# 可信端点与出站策略

## Purpose

定义 Java 应用连接可信本机、企业内网与公网 Hermes 服务时的可观察安全边界，使合法部署无需测试旁路，同时阻止未授权目标、身份外送及配置与实际连接之间的地址校验脱节。

## ADDED Requirements

### Requirement: EP-001 Explicit trusted endpoint selection

SDK MUST 由管理员配置的端点策略显式区分可信本地、可信私网与公网 HTTPS 目标；策略 MUST 限定主机及端口。旧配置入口 MUST NOT 自动放宽为允许任意私网；生产示例 MUST NOT 依赖仅供测试的地址旁路。

#### Scenario: EP-001-S1 Trusted local endpoint
- **GIVEN** 管理员仅授权 127.0.0.1 的测试服务端口
- **WHEN** 调用普通健康查询
- **THEN** 请求可以建立且无需测试旁路；其他未授权本机端口仍被拒绝。

#### Scenario: EP-001-S2 Untrusted input is not administrator policy
- **GIVEN** 最终用户提交一个未授权私网 URL
- **WHEN** 应用试图将其直接作为运行目标
- **THEN** SDK 在发送鉴权头和请求正文前报告端点策略错误，不将该 URL 自动加入可信集合。

### Requirement: EP-002 Bind checks to actual connection targets

SDK MUST 对实际连接使用的候选地址执行当前策略检查；严格策略 MUST NOT 在解析失败时声称地址已验证。代理代为解析而客户端无法证明目标地址时，SDK MUST 明确暴露该保证边界并要求可信代理策略，不能把预检查结果当作实际连接的证明。

#### Scenario: EP-002-S1 Mixed address resolution
- **GIVEN** 严格策略下同一主机解析到公网和未授权环回地址
- **WHEN** 连接器准备使用候选地址
- **THEN** 连接被拒绝且不发送敏感数据，不能只检查第一个安全地址。

#### Scenario: EP-002-S2 Resolution failure
- **GIVEN** 严格策略无法完成名称解析
- **WHEN** 请求准备发出
- **THEN** 返回可分类的解析或策略失败，不返回校验成功，不回退至未经授权地址。

### Requirement: EP-003 Prevent credential forwarding and path confusion

SDK MUST 默认拒绝跨 origin 跳转；MUST 拒绝 URL user-info 与用于鉴权的 URL 令牌；MUST 将模型、会话和运行标识作为独立路径段编码，防止标识改变 origin、路径层级或查询含义。鉴权只能由绑定身份提供。

#### Scenario: EP-003-S1 Cross origin redirect
- **GIVEN** 已绑定凭据的 HTTP 或 SSE 请求收到指向其他 origin 的跳转
- **WHEN** 客户端处理跳转
- **THEN** 不向新 origin 发送凭据或自动重放写请求，并返回清晰的跳转拒绝结果。

#### Scenario: EP-003-S2 Identifier contains reserved characters
- **GIVEN** 调用者提供包含斜线、空格或问号的对象标识
- **WHEN** 构造按标识查询的路径
- **THEN** 标识按端点契约编码为数据，不改变已授权目标和其他路径段。