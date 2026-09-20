# 三版本线与证据化发布

## Purpose

保证 Java 8、17、21 三条 SDK 版本线具有相同的公开业务契约，同时允许底层依赖适配差异；真实协议来源、运行环境、迁移变化与发布证据必须可追溯，而不是以单一 Mock 结果代替兼容证明。

## ADDED Requirements

### Requirement: BC-001 Maintain equivalent cross branch behavior

新增公开功能 MUST 在 feature/1.0.x、feature/2.0.x、feature/3.0.x 保持等价的请求字段、事件、错误与取消语义；分别维持 Java 8、17、21 基线。新增基础公开 API MUST NOT 强制高版本 JDK 类型或新的 Spring/Reactor 依赖。

#### Scenario: BC-001-S1 Same fixtures on all branches
- **GIVEN** 同一来源版本的协议样本与公开行为断言
- **WHEN** 分别在三条真实目标 JDK 上执行
- **THEN** 结果等价；仅批准的历史依赖类型差异允许进入白名单。

#### Scenario: BC-001-S2 Optional adapter requires newer JDK
- **GIVEN** 可选协议依赖不能在 Java 8 装载
- **WHEN** 发布基础 SDK 和可选适配器
- **THEN** 基础 Java 8 客户端仍能装载及执行已承诺功能，不因可选依赖隐式提高运行基线。

### Requirement: BC-002 Bind claimed support to verifiable evidence

发布所承诺的协议及平台支持 MUST 记录 SDK commit、目标 Hermes 版本/commit、实际 JDK、操作系统、依赖、命令与结果。人工边界样本、文档示例和真实捕获 MUST 区分。缺少证据的能力 MUST 标记未验证，不得将规范校验通过当作 SDK 测试通过。

#### Scenario: BC-002-S1 Only a mock test exists
- **GIVEN** 某协议仅有人工样本测试
- **WHEN** 生成兼容或发布报告
- **THEN** 报告说明测试层次及来源，不宣称已通过真实 Hermes 互操作。

#### Scenario: BC-002-S2 Unverified target platform
- **GIVEN** 进程清理仅在一种操作系统得到证明
- **WHEN** 声明跨平台支持范围
- **THEN** 未验证平台明确标记，不复制已验证平台的通过结论。

### Requirement: BC-003 Publish explicit migration and stage gates

每个变更批次 MUST 同步三分支测试、兼容矩阵和迁移说明。旧常用入口 MUST 通过兼容委托保留；确需删除的入口 MUST 另行声明 BREAKING 并经过兼容评审。但危险 POST 重放、凭据混用等安全行为 MUST NOT 作为默认兼容路径保留。计划阶段 MUST NOT 勾选实现任务或归档未实现的规范。

#### Scenario: BC-003-S1 Behavior changes with old signature
- **GIVEN** 旧方法签名仍存在但默认重连或凭据行为变严
- **WHEN** 发布候选版本
- **THEN** 说明变化、迁移入口和回滚边界，不声称完全无感兼容。

#### Scenario: BC-003-S2 One branch missing evidence
- **GIVEN** 其他两条版本线通过而第三条缺少回归结果
- **WHEN** 尝试宣布批次完成或归档
- **THEN** 发布门禁阻止完成声明，未验证任务保持未完成。