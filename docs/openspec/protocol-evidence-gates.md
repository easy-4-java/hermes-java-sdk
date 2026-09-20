# 实施前的协议采证门禁

本文件将未确认的外部信息变成具体前置工作，不把未知问题藏进实现。待实施代码只需满足“不支持/未知”行为即可保持安全；不能提前承诺未固定 wire 的高级能力。

| 门禁 | 需要固定的事实 | 产物 | 责任任务 | 未满足时 |
|---|---|---|---|---|
| G0 | 三分支源码、实际 JDK/Maven/依赖字节码 | verification/source-manifest.json | H-001 | 不声称三线构建通过 |
| G1 | 授权目标 Hermes commit/version、启动方式与鉴权 | verification/server-compatibility.json | H-001/H-002 | 真实互操作保持未完成 |
| G2 | API Chat/Responses/Run/Session 帧、终态、ID/游标 | src/test/resources/contracts/http/ 与 manifest | H-003/H-103/H-202 | 未确认解码能力为 UNKNOWN，禁止猜终态 |
| G3 | 创建 Run 幂等键/载荷冲突/身份范围和 stop 应答 | 同上与运行控制 descriptor | H-203 | 不自动重试未知写，不提前报告停止 |
| G4 | 审批原生挑战、决定、过期与拒绝表达 | approval descriptor/captures | H-204 | 不自动批准，不伪造 exactly-once |
| G5 | 模型选择/会话/Jobs patch 和 capability/health 形状 | resources descriptor/captures | H-205 | 高层不支持或未知；raw 仍受安全约束 |
| G6 | CLI 版本、只读帮助、机器模式、事件及退出码 | docs/compatibility/cli-command-matrix.md | H-301/H-302/H-304 | 不启动猜测命令或宣称交互完成 |
| G7 | ACP 版本、framing、双向 RPC、能力、权限选项 | docs/compatibility/acp-contract.md | H-401 | 不发送猜测 RPC；基础 SDK 不受影响 |
| G8 | Linux/macOS/Windows 所有权及后代回收能力 | verification/cli-runtime/ | H-303 | 未验证平台标 LIMITED/UNVERIFIED |

门禁以固定来源、脱敏样本、可复现命令和测试结果为证据。不得为通过 CI 关闭全局权限控制或调用有副作用的生产工具；付费模型测试需显式预算和隔离环境。缺失真实实例不妨碍规格文档写作，但阻止相应实现/发布通过声明。