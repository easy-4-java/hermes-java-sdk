# 场景到测试的执行蓝图

本文件是计划，不是已实现的测试报告。所有测试类/方法均须在实施任务中创建。每个方法继承对应 spec 场景的 GIVEN/WHEN/THEN，断言不能根据实现放宽。下面给出精确目标文件、方法、输入和可观察断言。

## request-context

目标文件：`src/test/java/io/github/easy4j/hermes/context/RequestContextContractTest.java`。
运行：`mvn -B -Dtest=RequestContextContractTest test`。

### CT-001-S1 → `testCt001S1`
准备：同一个请求构建对象将在发送后被另一线程修改。
动作：SDK 创建本次传输请求。
断言：已发送操作继续使用固定快照，之后的修改只影响后续明确创建的操作。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

### CT-001-S2 → `testCt001S2`
准备：一次请求指定不同的会话 ID 和记忆范围键。
动作：创建 Chat、Run 或会话操作。
断言：两种标识按各自契约传输，不互相覆盖，也不将其等同于平台授权。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

### CT-002-S1 → `testCt002S1`
准备：客户端有默认模型和 provider。
动作：请求显式指定另一组有效选择。
断言：服务端收到本次显式字段；未指定的可用默认值按约定补齐。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

### CT-002-S2 → `testCt002S2`
准备：请求模型 A，响应声明实际模型 B。
动作：SDK 构造运行结果。
断言：同时保留请求选择和实际 B，不将 A 写成实际执行模型。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

### CT-003-S1 → `testCt003S1`
准备：当前端点未确认支持某媒体输入。
动作：高层请求使用该输入。
断言：返回不支持或能力未知，不静默丢字段后改成文本请求。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

### CT-003-S2 → `testCt003S2`
准备：调用者使用高级扩展字段或请求头。
动作：扩展试图改变 Profile 或认证。
断言：安全检查仍拒绝冲突；普通合法扩展被保留。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `request-context/spec.md`。

## responses-streaming

目标文件：`src/test/java/io/github/easy4j/hermes/api/ResponsesStreamingContractTest.java`。
运行：`mvn -B -Dtest=ResponsesStreamingContractTest test`。

### RS-001-S1 → `testRs001S1`
准备：调用者在普通 Responses 方法中开启 stream。
动作：准备发送请求。
断言：请求未发送即报告参数与入口不匹配，并指引专用流式入口。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

### RS-001-S2 → `testRs001S2`
准备：目标服务支持 Responses 流。
动作：通过专用入口提交。
断言：进程或 HTTP 调用未结束前即可观察事件，并取得独立取消/观察句柄。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

### RS-002-S1 → `testRs002S1`
准备：两个输出项交错到达文本和工具活动。
动作：累积器生成结果。
断言：每个内容归入正确项，工具记录独立保留，最终文本不混入过程日志。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

### RS-002-S2 → `testRs002S2`
准备：最后一条权威结果包含此前已发出的增量文本。
动作：终态及重复终态到达。
断言：最终结果不重复拼接，完成一次且没有客户端二次工具执行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

### RS-003-S1 → `testRs003S1`
准备：服务端返回合法成功终态且无文本。
动作：构建最终结果。
断言：成功状态成立，空输出不被误判为网络错误。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

### RS-003-S2 → `testRs003S2`
准备：已接收若干输出项但连续性无法证明。
动作：连接恢复后获取运行最终状态。
断言：运行终态与观察完整性分别报告，不宣称此前流已无损恢复。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `responses-streaming/spec.md`。

## run-lifecycle

目标文件：`src/test/java/io/github/easy4j/hermes/runtime/RunLifecycleContractTest.java`。
运行：`mvn -B -Dtest=RunLifecycleContractTest test`。

### RN-001-S1 → `testRn001S1`
准备：宿主持有原运行标识及授权身份。
动作：重新创建观察句柄。
断言：只查询/订阅该运行，创建 Run 的计数不增加。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-001-S2 → `testRn001S2`
准备：绑定身份无法读取某运行。
动作：查询返回不存在或无权限。
断言：暴露对应错误，不推断其他 Profile 是否拥有该运行，也不报告成功。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-002-S1 → `testRn002S1`
准备：运行仍在执行。
动作：本地等待达到截止时间。
断言：返回观察超时，不调用远端停止，不改变服务端状态；之后仍可重新观察。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-002-S2 → `testRn002S2`
准备：服务端返回尚未识别的状态字符串。
动作：SDK 更新快照。
断言：保留原始状态并标记未知，不擅自选择任何终态。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-003-S1 → `testRn003S1`
准备：服务端支持幂等且本次键 K 已接受。
动作：响应丢失后执行允许的重试。
断言：继续使用 K 和同一载荷/身份，核对原操作，不创建第二个业务运行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-003-S2 → `testRn003S2`
准备：宿主复用键 K 但传入另一载荷。
动作：服务端或客户端检测冲突。
断言：返回幂等冲突，绝不自动生成替代键规避冲突。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-004-S1 → `testRn004S1`
准备：停止接口返回已接收。
动作：运行仍报告正在停止。
断言：句柄继续观察至真实终态，不提前宣布已停止。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

### RN-004-S2 → `testRn004S2`
准备：运行在停止提交前后自然完成。
动作：服务端确认最终完成。
断言：报告实际完成状态和停止请求结果，不强制改写为取消。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `run-lifecycle/spec.md`。

## approval-workflow

目标文件：`src/test/java/io/github/easy4j/hermes/runtime/ApprovalWorkflowContractTest.java`。
运行：`mvn -B -Dtest=ApprovalWorkflowContractTest test`。

### AP-001-S1 → `testAp001S1`
准备：收到审批请求但无处理器或处理器抛错。
动作：SDK 处理挑战。
断言：报告审批未完成/拒绝或按已确认契约拒绝，不自动发送允许。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `approval-workflow/spec.md`。

### AP-001-S2 → `testAp001S2`
准备：决定来自运行 A 的挑战。
动作：被用于运行 B 或其他 Profile。
断言：SDK 拒绝提交，不把它降级成不带绑定的通用决定。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `approval-workflow/spec.md`。

### AP-002-S1 → `testAp002S1`
准备：宿主异步处理审批。
动作：服务端继续发送控制和状态事件。
断言：读循环继续处理事件；审批完成后仅更新挑战处理，运行仍等待其真实终态。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `approval-workflow/spec.md`。

### AP-002-S2 → `testAp002S2`
准备：同一决定重复提交或挑战已过期。
动作：SDK 或服务端返回冲突/过期。
断言：明确报告拒绝、冲突或结果未知，不自动扩大授权或再提交新挑战。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `approval-workflow/spec.md`。

## runtime-resources

目标文件：`src/test/java/io/github/easy4j/hermes/api/RuntimeResourcesContractTest.java`。
运行：`mvn -B -Dtest=RuntimeResourcesContractTest test`。

### RR-001-S1 → `testRr001S1`
准备：服务端返回有限页和继续信息。
动作：调用者按页读取会话历史。
断言：边界与继续信息正确保留，不伪装为一次返回全部数据。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

### RR-001-S2 → `testRr001S2`
准备：目标契约已确认模型选择元数据接口。
动作：查询 provider/model 选择信息。
断言：返回类型化选项和原始扩展字段；请求默认值与服务端实际执行信息仍分开。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

### RR-002-S1 → `testRr002S1`
准备：现有 Job 含工作目录。
动作：一个更新省略该字段，另一个显式清空。
断言：前者不修改原值；后者按已验证的清空语义传输；两者序列化不同。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

### RR-002-S2 → `testRr002S2`
准备：宿主只要求一次执行或委托 Hermes Cron。
动作：SDK 接收该请求。
断言：仅执行所选一种职责，不另外登记重复定时任务。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

### RR-003-S1 → `testRr003S1`
准备：服务能力查询超时。
动作：高层自动化判断可否使用某功能。
断言：状态为未知并要求显式处理，不隐式回退到另一协议再执行。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

### RR-003-S2 → `testRr003S2`
准备：服务可达但返回认证失败。
动作：构造健康报告。
断言：报告鉴权失败，不能标记为网络离线或对话模型不可用。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-resources/spec.md`。

## runtime-observability

目标文件：`src/test/java/io/github/easy4j/hermes/observability/RuntimeObservabilityContractTest.java`。
运行：`mvn -B -Dtest=RuntimeObservabilityContractTest test`。

### OB-001-S1 → `testOb001S1`
准备：删除操作被服务端拒绝。
动作：调用类型化删除入口。
断言：返回权限错误而不是 false 或成功，保留受限诊断与关联信息。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。

### OB-001-S2 → `testOb001S2`
准备：限流应答给出 Retry-After。
动作：所需等待超过本次截止时间。
断言：返回限流/截止结果，不超期等待或重放无幂等保障的写请求。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。

### OB-002-S1 → `testOb002S1`
准备：运行结果不含缓存 token 或费用。
动作：构造用量结果。
断言：字段保持未提供，不用零或猜测值冒充服务端事实。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。

### OB-002-S2 → `testOb002S2`
准备：相同运行终态携带相同用量重复到达。
动作：句柄完成并发布最终结果。
断言：最终用量不倍增；跨进程结算仍要求宿主稳定身份防重。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。

### OB-003-S1 → `testOb003S1`
准备：错误与环境包含测试密钥、Cookie 和用户正文。
动作：默认日志及主动 BODY 调试分别记录。
断言：默认不记录正文；敏感字段均脱敏；开启正文仍限长，原始数据不自动外送。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。

### OB-003-S2 → `testOb003S2`
准备：应用没有安装观测后端。
动作：执行 HTTP、CLI 或 ACP 工作。
断言：核心行为仍正常；观察者失败被隔离而不阻塞关键资源清理。
证据：记录测试命令、目标分支/实际 JDK、用到的样本来源及 Surefire 结果；来源规范 `runtime-observability/spec.md`。