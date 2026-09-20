# Profile 身份与凭据隔离

## Purpose

定义命名 Profile 的凭据绑定、轮换和并发隔离，使连接复用不改变调用身份；普通请求选项不得覆盖已绑定认证，缺失凭据、身份变更和关闭行为均向宿主明确报告。

## ADDED Requirements

### Requirement: PA-001 Bind credentials to endpoint and profile

SDK MUST 将命名 Profile 与端点身份及凭据身份绑定；MUST 支持显式凭据和宿主提供的 Profile 凭据解析。命名 Profile 无可用凭据时 MUST 明确失败，MUST NOT 静默使用根 Profile 的密钥。

#### Scenario: PA-001-S1 Independent profiles
- **GIVEN** 同一服务的两个命名 Profile 配置不同测试密钥
- **WHEN** 两个 Profile 并发查询和订阅
- **THEN** 各请求只携带对应 Profile 的密钥；路径与凭据绑定一致。

#### Scenario: PA-001-S2 Missing profile credential
- **GIVEN** 根客户端有密钥而命名 Profile 没有凭据解析结果
- **WHEN** 创建或首次调用命名 Profile 视图
- **THEN** 返回配置错误，不借用根密钥发出请求。

### Requirement: PA-002 Rotate credentials without replaying unknown writes

SDK MUST 为每次新请求解析绑定身份当前可用的凭据；能力缓存及身份相关视图 MUST 按端点、Profile、凭据身份和必要的轮换标识隔离。凭据轮换 MUST NOT 自动重放结果未知的写请求，也 MUST NOT 改变其幂等身份范围。

#### Scenario: PA-002-S1 New requests after rotation
- **GIVEN** Profile 的凭据从版本 A 轮换为 B
- **WHEN** 轮换后创建新请求
- **THEN** 新请求使用 B；旧的身份相关缓存失效或重新核对；密钥正文不作为缓存键。

#### Scenario: PA-002-S2 Rotation after lost response
- **GIVEN** 写请求可能已被服务端接收但响应丢失
- **WHEN** 此时发生凭据轮换
- **THEN** SDK 报告结果未知或按已确认契约核对，不在新身份下重新创建操作。

### Requirement: PA-003 Isolate authentication from shared transport customization

SDK MUST 防止普通附加请求头覆盖 Authorization、Host、Cookie 身份或既定 Profile 路由。共享传输 MUST NOT 导致身份相关 Cookie、缓存、认证拦截行为跨 Profile 泄露。无法证明安全的外部定制 MUST 被拒绝用于隔离视图或要求显式隔离适配。

#### Scenario: PA-003-S1 Attempt to override identity
- **GIVEN** 请求绑定 Profile A
- **WHEN** 普通请求选项携带 Profile B 的认证头
- **THEN** 覆盖被拒绝并报告冲突，不能静默将请求变成 B 的身份。

#### Scenario: PA-003-S2 Shared transport with identity state
- **GIVEN** 调用者注入含认证状态的外部传输
- **WHEN** 尝试派生多个隔离 Profile
- **THEN** SDK 使用隔离策略或拒绝该定制，不声明未经证明的跨 Profile 隔离。