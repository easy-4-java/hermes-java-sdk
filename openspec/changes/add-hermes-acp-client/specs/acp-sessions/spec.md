# ACP 会话与能力边界

## Purpose

让 Java 宿主通过协商后的 ACP 能力创建会话、提交 prompt、观察消息和工具活动并取消当前轮；同时明确恢复、分叉、媒体和工具范围并非默认与 HTTP 等价，不支持能力不得通过隐式协议回退掩盖。

## ADDED Requirements

### Requirement: AS-001 Complete the supported session prompt lifecycle

SDK MUST 支持所选 ACP 契约下的创建会话、提交文本 prompt、读取通知、等待真实完成及取消当前轮闭环；本地观察中止与协议取消 MUST 分开。创建后的会话和原生执行关联信息 MUST 保留。

#### Scenario: AS-001-S1 Prompt completes with events
- **GIVEN** 协商成功并创建会话
- **WHEN** 提交文本且接收工具/消息通知直至完成
- **THEN** 通知在完成前可消费，结果保持会话关联并只完成一次。

#### Scenario: AS-001-S2 Cancel current turn
- **GIVEN** 会话存在进行中的 prompt
- **WHEN** 宿主明确请求协议取消
- **THEN** 只取消目标轮并核对结果，不擅自删除会话或终止其他会话。

### Requirement: AS-002 Gate optional operations and preserve native identity

恢复、分叉、列出会话、多模态及工具范围 MUST 按真实协商能力暴露。SDK MUST NOT 因 ACP 不支持某操作就自动通过 HTTP/CLI 重做。事件 MUST 保留原生调用身份；同名并行工具不能仅按名称关联完成。

#### Scenario: AS-002-S1 Restore not advertised
- **GIVEN** 服务端未声明恢复能力
- **WHEN** 调用恢复入口
- **THEN** 明确返回不支持或未知，不新建会话冒充恢复，不自动提交原 prompt。

#### Scenario: AS-002-S2 Parallel same named tools
- **GIVEN** 两个同名工具调用具有不同原生 ID
- **WHEN** 其完成事件乱序到达
- **THEN** 按照原生 ID 关联；缺少 ID 时明确关联不确定，不猜测配对。

### Requirement: AS-003 Keep ACP optional and preserve base SDK compatibility

ACP 集成 MUST 显式启用且不得阻塞基础 HTTP/有限 CLI 的装载和运行。基础 SDK 的 Java 基线 MUST 不因 ACP 依赖提高；不能满足某版本线时 MUST 采用可选分发边界并明确声明，不虚报三线 ACP 功能完全等价。

#### Scenario: AS-003-S1 ACP absent
- **GIVEN** 普通 Java 8 应用未安装任何 ACP 运行环境
- **WHEN** 仅使用基础 HTTP 功能
- **THEN** 核心 SDK 正常装载和工作，不启动探测进程或抛可选依赖缺失错误。

#### Scenario: AS-003-S2 Optional adapter needs different runtime
- **GIVEN** 选定 ACP 协议实现有额外依赖限制
- **WHEN** 构建和声明兼容矩阵
- **THEN** 依赖隔离且支持范围准确；基础构件不隐式携带高版本字节码。