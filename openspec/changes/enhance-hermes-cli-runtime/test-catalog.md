# 场景到测试的执行蓝图

本文件是计划，不是已实现的测试报告。所有测试类/方法均须在实施任务中创建。每个方法继承对应 spec 场景的 GIVEN/WHEN/THEN，断言不能根据实现放宽。下面给出精确目标文件、方法、输入和可观察断言。

## cli-execution

目标文件：`src/test/java/io/github/easy4j/hermes/cli/process/CliExecutionContractTest.java`。
运行：`mvn -B -Dtest=CliExecutionContractTest test`。

### CE-001-S1 → `testCe001S1`
准备：两个执行请求使用不同 Profile 和工作目录。
动作：同时启动两个测试进程。
断言：各自读取自己的配置快照，父进程和全局默认 Profile 不被修改。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-001-S2 → `testCe001S2`
准备：命令需要交互终端或浏览器授权。
动作：调用无交互有限执行入口。
断言：明确报告需要宿主交互或不支持，不声称授权已完成。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-002-S1 → `testCe002S1`
准备：prompt 含空格、中文、引号和分号。
动作：通过普通执行入口发送。
断言：子进程收到一个完整 prompt 参数，字符不被 shell 解释为额外命令。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-002-S2 → `testCe002S2`
准备：父进程含未授权秘密环境变量。
动作：创建受限子进程。
断言：变量不被隐式继承，日志也不输出整个父进程环境。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-003-S1 → `testCe003S1`
准备：测试进程持续输出超过声明上限。
动作：缓冲达到限制。
断言：返回输出超限错误、停止或回收拥有进程，内存不随输出无界增长。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-003-S2 → `testCe003S2`
准备：并发名额已用完且请求在有界队列中。
动作：请求被取消或截止时间到达。
断言：请求不启动，队列和配额被清理，不影响其他排队项。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-004-S1 → `testCe004S1`
准备：受管测试进程忽略协议取消。
动作：宽限时间到达。
断言：按该平台已验证方式终止并检查残留，结果包括取消与回收状态。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

### CE-004-S2 → `testCe004S2`
准备：SDK 仅观察宿主拥有的长期进程。
动作：关闭观察句柄。
断言：本地句柄和读写资源释放，但不终止该外部进程。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-execution/spec.md`。

## cli-streaming

目标文件：`src/test/java/io/github/easy4j/hermes/cli/stream/CliStreamingContractTest.java`。
运行：`mvn -B -Dtest=CliStreamingContractTest test`。

### CS-001-S1 → `testCs001S1`
准备：一条中文 JSONL 记录分多次读取且字符字节被拆开。
动作：读取至完整换行。
断言：恰好生成一个正确事件，前后合法空格保持。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-001-S2 → `testCs001S2`
准备：stdout 提供有效机器事件而 stderr 提供诊断。
动作：两路数据交错到达。
断言：诊断不进入事件 JSON 解析，两个通道独立且均受限。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-002-S1 → `testCs002S1`
准备：程序不存在或在协议初始化前退出。
动作：执行结束。
断言：返回启动失败、可用退出信息和受限 stderr，不一律报缺少 result。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-002-S2 → `testCs002S2`
准备：进程已发送文本事件但没有合法终态。
动作：进程退出。
断言：部分输出可访问但最终结果不完整，不按文本非空判断成功。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-002-S3 → `testCs002S3`
准备：收到成功终态后进程非成功退出。
动作：生成最终执行结果。
断言：保留终态与退出两份证据并报告冲突，不能伪造完整成功。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-003-S1 → `testCs003S1`
准备：测试 CLI 发出事件后延迟退出。
动作：监听者等待首个事件。
断言：首事件在进程结束前被观察到，有限执行的异步包装不能冒充此能力。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

### CS-003-S2 → `testCs003S2`
准备：最后结果文本包含已流式显示的内容。
动作：执行结果聚合。
断言：最终文本只保留一次，重复终态不重复计入用量。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-streaming/spec.md`。

## cli-commands

目标文件：`src/test/java/io/github/easy4j/hermes/cli/CliCommandsContractTest.java`。
运行：`mvn -B -Dtest=CliCommandsContractTest test`。

### CC-001-S1 → `testCc001S1`
准备：前序 Java 包装与目标文档的 cron 子命令名称存在差异。
动作：执行版本和帮助探测。
断言：记录目标真正接受的命令与别名；不创建真实任务验证猜测。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-commands/spec.md`。

### CC-001-S2 → `testCc001S2`
准备：宿主只查询安装命令是否可用。
动作：进行能力探测。
断言：不会安装插件或修改本地配置；仅返回有来源的支持信息。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-commands/spec.md`。

### CC-002-S1 → `testCc002S1`
准备：CLI 结果提供部分用量字段。
动作：SDK 映射类型化结果。
断言：保留来源与缺失状态，不从日志猜缓存消耗或账单。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-commands/spec.md`。

### CC-002-S2 → `testCc002S2`
准备：调用者绕过类型化命令而使用 raw argv。
动作：进程输出超过上限或排队超时。
断言：同样执行资源与取消策略，raw 入口不成为无限制执行通道。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `cli-commands/spec.md`。