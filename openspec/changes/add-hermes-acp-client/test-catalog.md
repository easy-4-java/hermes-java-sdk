# 场景到测试的执行蓝图

本文件是计划，不是已实现的测试报告。所有测试类/方法均须在实施任务中创建。每个方法继承对应 spec 场景的 GIVEN/WHEN/THEN，断言不能根据实现放宽。下面给出精确目标文件、方法、输入和可观察断言。

## acp-transport

目标文件：`src/test/java/io/github/easy4j/hermes/acp/AcpTransportContractTest.java`。
运行：`mvn -B -Dtest=AcpTransportContractTest test`。

### AT-001-S1 → `testAt001S1`
准备：启动支持选定 ACP 版本的进程。
动作：Java 发送初始化并读取对端结果。
断言：协商完成后才能发会话请求；stdout 仅作为协议通道，stderr 独立处理。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

### AT-001-S2 → `testAt001S2`
准备：对端不能接受 SDK 声明版本。
动作：收到初始化失败或不兼容应答。
断言：初始化明确失败且清理拥有资源，不伪装为可用客户端。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

### AT-002-S1 → `testAt002S1`
准备：并发发出两个请求，其间有会话通知。
动作：响应按相反顺序到达。
断言：每个响应完成正确请求；通知交付独立监听者，不按到达顺序猜关联。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

### AT-002-S2 → `testAt002S2`
准备：收到不属于任何挂起请求的响应。
动作：分发该响应。
断言：报告协议诊断并保留其他请求，不随意完成最早或同名请求。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

### AT-003-S1 → `testAt003S1`
准备：还有会话请求与权限应答未完成。
动作：ACP 进程异常退出。
断言：所有关联等待结束为进程/传输异常，无永久悬挂 Future。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

### AT-003-S2 → `testAt003S2`
准备：上次 prompt 可能已部分执行。
动作：宿主重启进程。
断言：不会自动重发 prompt；先初始化，仅按已支持能力显式恢复或重新创建。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-transport/spec.md`。

## acp-sessions

目标文件：`src/test/java/io/github/easy4j/hermes/acp/AcpSessionsContractTest.java`。
运行：`mvn -B -Dtest=AcpSessionsContractTest test`。

### AS-001-S1 → `testAs001S1`
准备：协商成功并创建会话。
动作：提交文本且接收工具/消息通知直至完成。
断言：通知在完成前可消费，结果保持会话关联并只完成一次。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

### AS-001-S2 → `testAs001S2`
准备：会话存在进行中的 prompt。
动作：宿主明确请求协议取消。
断言：只取消目标轮并核对结果，不擅自删除会话或终止其他会话。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

### AS-002-S1 → `testAs002S1`
准备：服务端未声明恢复能力。
动作：调用恢复入口。
断言：明确返回不支持或未知，不新建会话冒充恢复，不自动提交原 prompt。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

### AS-002-S2 → `testAs002S2`
准备：两个同名工具调用具有不同原生 ID。
动作：其完成事件乱序到达。
断言：按照原生 ID 关联；缺少 ID 时明确关联不确定，不猜测配对。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

### AS-003-S1 → `testAs003S1`
准备：普通 Java 8 应用未安装任何 ACP 运行环境。
动作：仅使用基础 HTTP 功能。
断言：核心 SDK 正常装载和工作，不启动探测进程或抛可选依赖缺失错误。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

### AS-003-S2 → `testAs003S2`
准备：选定 ACP 协议实现有额外依赖限制。
动作：构建和声明兼容矩阵。
断言：依赖隔离且支持范围准确；基础构件不隐式携带高版本字节码。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-sessions/spec.md`。

## acp-permissions

目标文件：`src/test/java/io/github/easy4j/hermes/acp/AcpPermissionsContractTest.java`。
运行：`mvn -B -Dtest=AcpPermissionsContractTest test`。

### AM-001-S1 → `testAm001S1`
准备：工具请求权限但宿主未配置处理器。
动作：收到权限挑战。
断言：返回协定拒绝/取消或明确错误，不默认批准该工具执行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-permissions/spec.md`。

### AM-001-S2 → `testAm001S2`
准备：宿主仍持有旧连接的权限决定。
动作：尝试用于新连接上的请求。
断言：拒绝过期绑定，不能把旧授权扩展到新连接。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-permissions/spec.md`。

### AM-002-S1 → `testAm002S1`
准备：Java 正等待 prompt，服务端反向请求权限。
动作：宿主异步查询状态后作出决定。
断言：状态响应和通知能继续处理；权限应答可送达，形成真实双向闭环而非死锁。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-permissions/spec.md`。

### AM-002-S2 → `testAm002S2`
准备：权限等待已经超时并发送拒绝。
动作：异步宿主稍后返回允许。
断言：迟到允许不再发送，挑战只记录一个最终决定。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `acp-permissions/spec.md`。