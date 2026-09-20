# CLI 命令兼容与用量

## Purpose

为常用有限管理命令与用量结果提供逐版本、可验证的 Java 入口，通过无副作用探测说明已支持命令、选项和退出语义；保留高级 argv 的同时不把分组命令或交互界面当作完整管理集成。

## ADDED Requirements

### Requirement: CC-001 Discover command support without side effects

命令可用性探测 MUST 只使用版本、帮助或其他已确认只读方式；MUST NOT 为探测执行安装、删除、更新或服务启动。子命令、别名、选项与退出码 MUST 按固定目标版本核实，未知时明确报告。

#### Scenario: CC-001-S1 Probe cron command compatibility
- **GIVEN** 前序 Java 包装与目标文档的 cron 子命令名称存在差异
- **WHEN** 执行版本和帮助探测
- **THEN** 记录目标真正接受的命令与别名；不创建真实任务验证猜测。

#### Scenario: CC-001-S2 Plugin installation availability
- **GIVEN** 宿主只查询安装命令是否可用
- **WHEN** 进行能力探测
- **THEN** 不会安装插件或修改本地配置；仅返回有来源的支持信息。

### Requirement: CC-002 Expose typed options and factual machine usage

常用配置、Profile、Skills、Jobs 命令 MUST 提供经过契约验证的类型化选项；高级 argv 入口 MUST 保留上下文和资源限制。用量 MUST 来自机器可读字段，缺失保持缺失；MUST 区分实际模型、请求模型、进程时长和模型调用时长。

#### Scenario: CC-002-S1 Machine usage missing cache tokens
- **GIVEN** CLI 结果提供部分用量字段
- **WHEN** SDK 映射类型化结果
- **THEN** 保留来源与缺失状态，不从日志猜缓存消耗或账单。

#### Scenario: CC-002-S2 Raw command exceeds quota
- **GIVEN** 调用者绕过类型化命令而使用 raw argv
- **WHEN** 进程输出超过上限或排队超时
- **THEN** 同样执行资源与取消策略，raw 入口不成为无限制执行通道。