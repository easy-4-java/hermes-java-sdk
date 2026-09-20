# 场景到测试的执行蓝图

本文件是计划，不是已实现的测试报告。所有测试类/方法均须在实施任务中创建。每个方法继承对应 spec 场景的 GIVEN/WHEN/THEN，断言不能根据实现放宽。下面给出精确目标文件、方法、输入和可观察断言。

## trusted-endpoints

目标文件：`src/test/java/io/github/easy4j/hermes/security/EndpointPolicyContractTest.java`。
运行：`mvn -B -Dtest=EndpointPolicyContractTest test`。

### EP-001-S1 → `testEp001S1`
准备：管理员仅授权 127.0.0.1 的测试服务端口。
动作：调用普通健康查询。
断言：请求可以建立且无需测试旁路；其他未授权本机端口仍被拒绝。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

### EP-001-S2 → `testEp001S2`
准备：最终用户提交一个未授权私网 URL。
动作：应用试图将其直接作为运行目标。
断言：SDK 在发送鉴权头和请求正文前报告端点策略错误，不将该 URL 自动加入可信集合。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

### EP-002-S1 → `testEp002S1`
准备：严格策略下同一主机解析到公网和未授权环回地址。
动作：连接器准备使用候选地址。
断言：连接被拒绝且不发送敏感数据，不能只检查第一个安全地址。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

### EP-002-S2 → `testEp002S2`
准备：严格策略无法完成名称解析。
动作：请求准备发出。
断言：返回可分类的解析或策略失败，不返回校验成功，不回退至未经授权地址。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

### EP-003-S1 → `testEp003S1`
准备：已绑定凭据的 HTTP 或 SSE 请求收到指向其他 origin 的跳转。
动作：客户端处理跳转。
断言：不向新 origin 发送凭据或自动重放写请求，并返回清晰的跳转拒绝结果。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

### EP-003-S2 → `testEp003S2`
准备：调用者提供包含斜线、空格或问号的对象标识。
动作：构造按标识查询的路径。
断言：标识按端点契约编码为数据，不改变已授权目标和其他路径段。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `trusted-endpoints/spec.md`。

## profile-authentication

目标文件：`src/test/java/io/github/easy4j/hermes/security/ProfileAuthenticationContractTest.java`。
运行：`mvn -B -Dtest=ProfileAuthenticationContractTest test`。

### PA-001-S1 → `testPa001S1`
准备：同一服务的两个命名 Profile 配置不同测试密钥。
动作：两个 Profile 并发查询和订阅。
断言：各请求只携带对应 Profile 的密钥；路径与凭据绑定一致。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

### PA-001-S2 → `testPa001S2`
准备：根客户端有密钥而命名 Profile 没有凭据解析结果。
动作：创建或首次调用命名 Profile 视图。
断言：返回配置错误，不借用根密钥发出请求。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

### PA-002-S1 → `testPa002S1`
准备：Profile 的凭据从版本 A 轮换为 B。
动作：轮换后创建新请求。
断言：新请求使用 B；旧的身份相关缓存失效或重新核对；密钥正文不作为缓存键。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

### PA-002-S2 → `testPa002S2`
准备：写请求可能已被服务端接收但响应丢失。
动作：此时发生凭据轮换。
断言：SDK 报告结果未知或按已确认契约核对，不在新身份下重新创建操作。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

### PA-003-S1 → `testPa003S1`
准备：请求绑定 Profile A。
动作：普通请求选项携带 Profile B 的认证头。
断言：覆盖被拒绝并报告冲突，不能静默将请求变成 B 的身份。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

### PA-003-S2 → `testPa003S2`
准备：调用者注入含认证状态的外部传输。
动作：尝试派生多个隔离 Profile。
断言：SDK 使用隔离策略或拒绝该定制，不声明未经证明的跨 Profile 隔离。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `profile-authentication/spec.md`。

## sse-lifecycle

目标文件：`src/test/java/io/github/easy4j/hermes/api/sse/SseLifecycleContractTest.java`。
运行：`mvn -B -Dtest=SseLifecycleContractTest test`。

### SE-001-S1 → `testSe001S1`
准备：data 是包含 choices[0].delta.content 的合法聊天 JSON。
动作：消费含中文和空格的文本分片。
断言：文本被正确读取且空格不丢失，原始 data 可访问，不需要额外 data 包装。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-001-S2 → `testSe001S2`
准备：SSE 使用 CRLF、多行 data、注释并在 UTF-8 字符中间分片。
动作：网络分批读取并完成事件。
断言：仅完整帧交付解码，注释不成为业务事件，字符不损坏。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-002-S1 → `testSe002S1`
准备：业务 JSON 可成功解析。
动作：事件监听器抛出异常。
断言：报告消费者错误而非 JSON 错误，并按配置终止或隔离订阅且释放对应资源。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-002-S2 → `testSe002S2`
准备：服务端发送客户端未识别的事件类型。
动作：客户端接收该事件。
断言：保留原始类型与内容；普通未知事件可继续观察，未知必需控制事件不能被默认为批准或成功。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-003-S1 → `testSe003S1`
准备：服务端发送该端点的合法成功终态。
动作：随后 EOF 或重复终态到达。
断言：成功只完成一次，资源回收，原始聊天 POST 次数保持一次。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-003-S2 → `testSe003S2`
准备：已有文本增量但没有已验证终态。
动作：连接意外关闭。
断言：结果标记不完整；无可核对句柄时返回中断或结果未知，不能以部分文本成功结束。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-004-S1 → `testSe004S1`
准备：服务端已接受一轮会话聊天。
动作：响应流中途断开。
断言：客户端不再次提交原始 POST；可用句柄仅用于观察或核对，外部工具不会因自动重放再执行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-004-S2 → `testSe004S2`
准备：某写操作没有已验证幂等契约。
动作：发生可重试网络异常。
断言：不重放该写请求；报告提交阶段和结果不确定性。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-005-S1 → `testSe005S1`
准备：已有运行可查询但服务端不提供事件恢复游标。
动作：客户端重新附着。
断言：订阅和核对原运行，标记连续性未证明；不新建运行，不宣称所有中间事件已恢复。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-005-S2 → `testSe005S2`
准备：连续两个事件有不同服务端身份但相同文本。
动作：消费重连前后的事件。
断言：两个合法增量均保留；仅按已验证的事件身份/游标规则去重。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-006-S1 → `testSe006S1`
准备：有界队列已满且后续到达审批或终态。
动作：客户端无法继续可靠交付。
断言：明确报告溢出并关闭或核对观察，不丢控制事件后继续报告成功。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

### SE-006-S2 → `testSe006S2`
准备：订阅 A 的监听器阻塞而订阅 B 正常。
动作：两个订阅持续收到事件。
断言：A 的处理不阻塞 B 的传输，单个订阅内部顺序保持。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sse-lifecycle/spec.md`。

## request-cancellation

目标文件：`src/test/java/io/github/easy4j/hermes/transport/PublicCancellationContractTest.java`。
运行：`mvn -B -Dtest=PublicCancellationContractTest test`。

### RC-001-S1 → `testRc001S1`
准备：公开异步方法已发起尚未结束的网络调用。
动作：调用者取消 SDK 直接返回的 Future。
断言：对应调用终止、重试停止且取消注册释放；并发其他请求继续运行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

### RC-001-S2 → `testRc001S2`
准备：调用者从 SDK 结果自行派生新的 Future。
动作：仅取消该派生 Future 或改用共享令牌取消。
断言：文档不承诺派生取消反向传播；使用共享令牌时对应网络调用确定被取消。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

### RC-002-S1 → `testRc002S1`
准备：远端 Run 仍在运行。
动作：宿主关闭其事件观察句柄。
断言：本地连接关闭且不发送远端 stop，运行状态不被伪造为 cancelled。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

### RC-002-S2 → `testRc002S2`
准备：写请求已经发送。
动作：调用者取消网络请求。
断言：网络资源收敛；结果可能未知被显式标记，允许按身份及操作键核对。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

### RC-003-S1 → `testRc003S1`
准备：SDK 与宿主无关任务使用同一外部传输。
动作：关闭 SDK 两次。
断言：SDK 的工作清理一次；宿主无关任务和外部全局资源仍可使用。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

### RC-003-S2 → `testRc003S2`
准备：一个线程关闭客户端，另一个创建订阅或安排重连。
动作：这两个动作并发执行。
断言：创建操作被拒绝或随后被清理；无遗留重连、Future 或活动订阅。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-cancellation/spec.md`。

## sdk-compatibility

目标文件：`src/test/java/io/github/easy4j/hermes/compatibility/SdkCompatibilityContractTest.java`。
运行：`mvn -B -Dtest=SdkCompatibilityContractTest test`。

### BC-001-S1 → `testBc001S1`
准备：同一来源版本的协议样本与公开行为断言。
动作：分别在三条真实目标 JDK 上执行。
断言：结果等价；仅批准的历史依赖类型差异允许进入白名单。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。

### BC-001-S2 → `testBc001S2`
准备：可选协议依赖不能在 Java 8 装载。
动作：发布基础 SDK 和可选适配器。
断言：基础 Java 8 客户端仍能装载及执行已承诺功能，不因可选依赖隐式提高运行基线。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。

### BC-002-S1 → `testBc002S1`
准备：某协议仅有人工样本测试。
动作：生成兼容或发布报告。
断言：报告说明测试层次及来源，不宣称已通过真实 Hermes 互操作。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。

### BC-002-S2 → `testBc002S2`
准备：进程清理仅在一种操作系统得到证明。
动作：声明跨平台支持范围。
断言：未验证平台明确标记，不复制已验证平台的通过结论。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。

### BC-003-S1 → `testBc003S1`
准备：旧方法签名仍存在但默认重连或凭据行为变严。
动作：发布候选版本。
断言：说明变化、迁移入口和回滚边界，不声称完全无感兼容。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。

### BC-003-S2 → `testBc003S2`
准备：其他两条版本线通过而第三条缺少回归结果。
动作：尝试宣布批次完成或归档。
断言：发布门禁阻止完成声明，未验证任务保持未完成。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `sdk-compatibility/spec.md`。