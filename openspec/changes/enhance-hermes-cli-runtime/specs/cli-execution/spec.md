# CLI 执行上下文与有界资源

## Purpose

将有限命令、结构化单轮执行和长期受管进程分为清晰的调用形态，固定每次执行的 Profile、环境与工作目录，保证参数安全、输出和并发有界，并只回收 SDK 确实拥有的进程资源。

## ADDED Requirements

### Requirement: CE-001 Separate execution modes and snapshot process context

SDK MUST 区分有限执行、结构化流式执行和长期受管进程；旧有限入口 MUST NOT 悄悄改变为长连接。每次执行 MUST 固定程序、工作目录、允许环境及 Profile 上下文；MUST NOT 通过修改全局默认 Profile 切换并发请求身份。

#### Scenario: CE-001-S1 Two profiles execute concurrently
- **GIVEN** 两个执行请求使用不同 Profile 和工作目录
- **WHEN** 同时启动两个测试进程
- **THEN** 各自读取自己的配置快照，父进程和全局默认 Profile 不被修改。

#### Scenario: CE-001-S2 Interactive command in noninteractive mode
- **GIVEN** 命令需要交互终端或浏览器授权
- **WHEN** 调用无交互有限执行入口
- **THEN** 明确报告需要宿主交互或不支持，不声称授权已完成。

### Requirement: CE-002 Preserve argv boundaries and explicit environment policy

普通 CLI 调用 MUST 通过独立参数传递而非 shell 拼接，保留空格、中文及字面字符。环境继承及覆盖 MUST 遵循明确名单；秘密值 MUST NOT 出现在日志或命令诊断。高级 shell 执行不得混入普通入口。

#### Scenario: CE-002-S1 Prompt contains shell characters
- **GIVEN** prompt 含空格、中文、引号和分号
- **WHEN** 通过普通执行入口发送
- **THEN** 子进程收到一个完整 prompt 参数，字符不被 shell 解释为额外命令。

#### Scenario: CE-002-S2 Unapproved environment variable
- **GIVEN** 父进程含未授权秘密环境变量
- **WHEN** 创建受限子进程
- **THEN** 变量不被隐式继承，日志也不输出整个父进程环境。

### Requirement: CE-003 Bound output concurrency and admission waiting

SDK MUST 为 stdout、stderr、单条结构化记录、并发进程数及待执行队列配置有限上限。输出超限 MUST 明确失败并处理拥有的进程；排队 MUST 可取消且受 deadline 限制。所有退出路径 MUST 释放占用配额。

#### Scenario: CE-003-S1 Output floods memory budget
- **GIVEN** 测试进程持续输出超过声明上限
- **WHEN** 缓冲达到限制
- **THEN** 返回输出超限错误、停止或回收拥有进程，内存不随输出无界增长。

#### Scenario: CE-003-S2 Queued request cancelled
- **GIVEN** 并发名额已用完且请求在有界队列中
- **WHEN** 请求被取消或截止时间到达
- **THEN** 请求不启动，队列和配额被清理，不影响其他排队项。

### Requirement: CE-004 Cancel and close according to process ownership

SDK MUST 对拥有进程执行可观察的取消、宽限等待及必要系统终止并确认结果；MUST NOT 杀死仅附着的外部进程或无关进程。每个平台的子进程树清理支持 MUST 有独立证据；无法保证时明确报告，不作跨平台完整回收承诺。

#### Scenario: CE-004-S1 Owned process ignores cooperative cancellation
- **GIVEN** 受管测试进程忽略协议取消
- **WHEN** 宽限时间到达
- **THEN** 按该平台已验证方式终止并检查残留，结果包括取消与回收状态。

#### Scenario: CE-004-S2 Attached external process
- **GIVEN** SDK 仅观察宿主拥有的长期进程
- **WHEN** 关闭观察句柄
- **THEN** 本地句柄和读写资源释放，但不终止该外部进程。