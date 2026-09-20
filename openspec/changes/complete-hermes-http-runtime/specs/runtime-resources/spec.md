# 模型、会话、Jobs 与能力发现

## Purpose

为模型选项、会话、定时任务、健康与能力查询建立类型化且可扩展的外部契约，区分缺失与清空、不可见与失败、未知与不支持，并以服务端能力证据限制自动化操作。

## ADDED Requirements

### Requirement: RR-001 Expose typed model session and job operations

SDK MUST 为已验证的模型选择元数据、会话创建/更新/分叉/历史/分页及 Jobs 查询/修改/控制提供类型化访问，保留高级原始字段入口。未验证子能力 MUST 显式不支持或未知，MUST NOT 由方法名推定可用。

#### Scenario: RR-001-S1 Paged sessions
- **GIVEN** 服务端返回有限页和继续信息
- **WHEN** 调用者按页读取会话历史
- **THEN** 边界与继续信息正确保留，不伪装为一次返回全部数据。

#### Scenario: RR-001-S2 Verified model metadata
- **GIVEN** 目标契约已确认模型选择元数据接口
- **WHEN** 查询 provider/model 选择信息
- **THEN** 返回类型化选项和原始扩展字段；请求默认值与服务端实际执行信息仍分开。

### Requirement: RR-002 Preserve patch tri state and schedule ownership

任务增量修改 MUST 区分未设置、显式赋值与显式清空；时区、技能、工作目录和投递字段 MUST 按目标契约表达。SDK MUST NOT 隐式再启动一个本地调度器执行已注册的 Hermes 任务，也不得默认重放立即执行请求。

#### Scenario: RR-002-S1 Omit versus clear
- **GIVEN** 现有 Job 含工作目录
- **WHEN** 一个更新省略该字段，另一个显式清空
- **THEN** 前者不修改原值；后者按已验证的清空语义传输；两者序列化不同。

#### Scenario: RR-002-S2 External scheduler owns timing
- **GIVEN** 宿主只要求一次执行或委托 Hermes Cron
- **WHEN** SDK 接收该请求
- **THEN** 仅执行所选一种职责，不另外登记重复定时任务。

### Requirement: RR-003 Represent unknown capabilities and health failures explicitly

SDK MUST 区分能力支持、不支持与未知；能力缓存 MUST 遵守身份与版本边界。能力获取失败 MUST NOT 推定全部支持；响应中的端点信息 MUST NOT 授权访问新 origin。健康结果 MUST 区分存活、可用性、认证失败和传输失败。

#### Scenario: RR-003-S1 Capability fetch fails
- **GIVEN** 服务能力查询超时
- **WHEN** 高层自动化判断可否使用某功能
- **THEN** 状态为未知并要求显式处理，不隐式回退到另一协议再执行。

#### Scenario: RR-003-S2 Auth error on health
- **GIVEN** 服务可达但返回认证失败
- **WHEN** 构造健康报告
- **THEN** 报告鉴权失败，不能标记为网络离线或对话模型不可用。