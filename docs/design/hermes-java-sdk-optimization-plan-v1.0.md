# Hermes Java SDK 优化与能力补齐方案 V1.0

文档日期：2026-09-20  
适用仓库：`easy-4-java/hermes-java-sdk`  
文档状态：建议实施方案；尚未修改、提交或发布 SDK 代码。  
实施方向：协议正确性优先、公共接口渐进增强、三条 Java 版本线同步维护。

## 0. 结论与交付目标

本次优化不是再增加一批 CLI 包装方法，也不是重写 Hermes，而是将现有 SDK 建设为一个**协议可靠、生命周期清晰、可观测、可演进的 Hermes Java 接入层**。

建议选择“保留现有单 Maven 模块、逐步分离内部职责、兼容性扩展 API”的方案。第一批只处理可信端点、Profile 凭据、SSE 解码与重连、公开异步接口的取消链；第二批补齐 HTTP 运行闭环；第三批补齐 CLI 结构化流和进程管理；第四批实现 ACP，其他协议按真实业务需要立项。

最终验收不以 Java 方法数量或 Mock 测试绿灯为依据，而以以下结果为依据：一次 Agent 工作能够正确提交、观察、等待审批、请求停止、核对最终状态；断线不会盲目重复执行；身份不会跨 Profile 混用；三条 Java 版本线对相同协议报文产生等价的业务结果。

### 0.1 源码基线与证据边界

本方案固定以下审计基线，防止后续分支移动导致讨论对象不一致。[C0]

| 分支 | 审计提交 | 本方案要求保留的运行基线 |
|---|---|---|
| `feature/1.0.x` | `76710a9d7e72bd589d2244ecf9004a9bec3d3ed2` | Java 8 |
| `feature/2.0.x` | `c97b68698751b2560b2472ee96874ccb10494e46` | Java 17 |
| `feature/3.0.x` | `3b3f148a09987442c71586cec809a62529f4c940` | Java 21 |

协议风险主要以第三条分支的源码复核为依据；涉及共享行为的修复须在三个分支分别验证，不能仅因为之前源码相似就视为验证完成。Java 基线延续前序约定，执行时还需由各分支 POM、真实 JDK 运行和依赖字节码检查共同确认。

官方 `llms-full.txt` 超过本次网页读取上限，因此核对采用其对应的 API Server、CLI、ACP、Profiles、Cron、MCP、A2A 独立章节。这里不声称已逐字读取全文、不声称已执行 codegraph、不声称已连接真实 Hermes 运行实例。

正文中的类名、包名、接口和任务拆分，除明确指出“现有”者外，均为拟议设计，不是当前已存在的 SDK API。示例不作为可直接编译的现有代码。

## 1. 范围与架构决策

### 1.1 采用的方案

| 方案 | 优点 | 代价 | 决策 |
|---|---|---|---|
| 继续在大门面中添加方法 | 初始改动少 | 协议、生命周期、资源所有权问题继续累积 | 不采用 |
| 立即重写为多个 Maven 子模块 | 结构可从零整理 | 坐标、依赖、迁移与三分支成本同时扩大 | 不作为第一阶段 |
| 保留坐标和旧入口，内部按职责分层 | 可小批交付，风险可控 | 需要兼容适配与持续回归 | 采用 |

### 1.2 硬性边界

SDK 负责网络与本地协议、请求身份、模型映射、事件解码、资源生命周期、错误表达和观测接口。Hermes 负责 Agent Loop、工具实际执行、记忆、会话持久化和其自身的调度。宿主系统负责租户权限、企业审批规则、凭据存储、长期工作流以及经营计费。

不得因为 HTTP 不可用就自动改走 CLI 再执行一遍；不得因为 SSE 断线就自动发起新的 Agent 轮次；不得把服务端已经完成的工具活动重新在 Java 中执行。

不新增 Spring/Reactor 作为基础依赖，不强制所有使用者安装 ACP、A2A 或 MCP 相关组件，不把 Dashboard 内部接口、Python 回调或本地 SQLite 结构当成稳定公共协议。

## 2. 目标逻辑结构

```text
Java 应用 / 平台适配器
           │
     HermesClient（兼容门面）
           │
  ┌────────┼─────────┬────────┬──────────┐
  │        │         │        │          │
 Chat   Responses   Runs   Sessions/Jobs  CLI
  │        │         │        │          │
  └────────┴────┬────┴────────┘          │
                │                        │
  Context / Credentials / Capabilities   │
  Events / Lifecycle / Retry / Errors    │
                │                        │
         HTTP + SSE transport      Process + JSONL
                │                        │
                └─────────┬──────────────┘
                          │
                       Hermes

后续可选接入：ACP（双向 stdio）；A2A；MCP Bridge
```

以上是逻辑分层，不要求先拆 Maven 模块。保留已有包和公开类，先增加内部组件，再逐步委托。建议新增包职责：

| 包 | 职责 |
|---|---|
| `context` | 请求上下文、默认配置合并、身份绑定 |
| `security` | 可信端点、凭据解析、鉴权头保护 |
| `transport` | HTTP 执行、取消传播、资源所有权 |
| `api.sse` | 原始 SSE 帧、协议解码、订阅生命周期 |
| `runtime` | RunHandle、状态核对、停止与审批 |
| `cli.stream` | JSONL 解析、流式结果、事件消费 |
| `cli.process` | 有界进程执行、环境、取消和退出 |
| `observability` | 无强制遥测依赖的观察者接口 |
| `acp` | 后续 ACP 会话与 JSON-RPC 适配 |

新的公共模型优先使用 Java 8 能表达的普通类、接口、集合和 CompletableFuture。新增公共 API 不暴露 Jackson 2/3 特有类型；原有暴露 Jackson 或 OkHttp 的构造器暂不删除。

## 3. 第一批：协议与安全正确性

### 3.1 可信端点策略

**已见问题：**现有 EndpointGuard 将环回与私网地址归为不安全目标；其 DNS 检查只解析一个地址，解析失败会直接返回。这不足以同时表达“可信本地服务”和“严格公网出口”的不同要求。[C1]

**设计：**新增 EndpointPolicy，并通过显式的配置入口选择策略。

| 模式 | 允许范围 | 限制 |
|---|---|---|
| `TRUSTED_LOCAL` | 管理员指定的环回 IP 与端口 | 保留身份认证；不扩展为任意本机端口 |
| `TRUSTED_PRIVATE` | 指定主机、端口和必要的网段 | 必须明确授权，不能来自普通用户任意输入 |
| `PUBLIC_HTTPS` | 符合策略的公网 HTTPS 目标 | 验证 TLS、实际解析地址与跳转目标 |

为避免悄悄放宽旧配置，旧入口继续保持明确的严格策略；使用本地 Hermes 的调用方迁移到正式的可信本地入口，而不是调用测试旁路方法。

执行规则：配置中声明预期 origin；禁止 URL 中夹带用户信息或访问令牌；默认不跟随跨 origin 跳转；路径参数用路径段编码而不是字符串拼接。DNS 校验应与实际连接使用的解析器绑定，并检查所有候选地址；不能“校验一次后由另一个解析器重新解析”。企业代理下本地无法证明的远端解析保证，要交给可信代理/网络出口控制并在文档中明确。

`markUnsafeBaseUrlOverriddenForTest` 不再出现在生产示例中。旧公开方法先弃用，测试逐步迁移至测试专用配置，不在修复版直接删除造成源代码不兼容。

**验收：**显式本地配置可连测试服务；没有配置的端口、私网目标或跳转被拒绝；凭据不随跨 origin 请求发送；HTTP 与 SSE 使用同一策略；失败发生在发送敏感请求前。

### 3.2 Profile 与凭据绑定

**现有形态：**`forProfile()` 缓存托管视图，复制 HTTP 配置后修改 URL 前缀。[C2] 官方命名 Profile 使用其自己的鉴权密钥。[S1]

**拟议接口职责：**

```text
ProfileBinding
  endpointIdentity
  profileId
  credentialIdentity（非密钥正文）
  credentialProvider

CredentialProvider
  按绑定身份解析当前凭据
  可返回用于缓存失效的 generation
```

新增带凭据的 `forProfile` 重载，并允许根客户端配置 ProfileCredentialResolver。旧 `forProfile(String)` 在 resolver 存在时继续使用；resolver 缺失时对命名 Profile 给出明确配置错误，不静默借用根密钥。

共享连接池并不意味着共享 Authorization、CookieJar、响应缓存、Profile 状态或捕获旧密钥的 interceptor。客户端视图和能力缓存至少按 endpoint、profile、credentialIdentity 隔离；轮换依据 credential generation 失效，不使用原始令牌作为缓存键或日志字段。

普通附加 headers 不得覆盖 Authorization、Host 或既定 Profile 路由。特殊传输定制必须经明确的高级接口接入，不能让 RequestOptions 成为任意鉴权旁路。

**验收：**两套独立凭据并发调用不串用；凭据缺失明确失败；轮换后新请求使用新凭据；根客户端关闭清理其拥有的托管资源，但不终止外部客户端的无关调用。

### 3.3 SSE 两层解码

**已见问题：**现有 onEvent 将原始 data 直接反序列化为 SseEvent，再读取其中 data；同时解析和用户回调处于同一异常捕获块，终态主要依赖 `[DONE]`。[C3]

**设计：**不再让传输层假设业务 JSON 自带 `data` 字段。

```text
SseFrame
  eventId（可空）
  eventName（可空）
  rawData
  receivedAt

SseFrame → EndpointSpecificDecoder → TypedEvent
```

分别实现 Chat、Responses、Run、Session 的解码器。沿用底层 SSE 客户端处理传输分帧和注释，不能自行对网络字节按任意换行截断。额外解析器必须通过多行 data、CRLF、注释、UTF-8 分片和超大帧测试。

统一事件只提供公共包络：来源协议、原始类型、run/session 标识、可用的服务端序号、接收时间和可访问的原始载荷。服务端没有给 ID 时不能伪造“服务端事件 ID”；本地接收序号必须另行命名。

建议业务类别包括文本增量、过程消息、工具开始/结束、审批请求、运行终态、用量信息、未知事件。只有服务端确实提供且允许展示的内容才映射到对应类别；不生成或推断服务端未提供的推理信息。工具、过程消息和最终答案分别累积。

默认严格区分协议解析失败、消费者异常、网络失败和业务失败。未知事件可保留原始报文，但未知的必需控制语义不得假装已处理。回调失败应按策略隔离或终止订阅；所有清理必须在 finally 或等价路径完成。

### 3.4 重连、终态与有界队列

**已见问题：**Session 聊天 POST 在当前实现中启用了重连，关闭时会重新构造请求；队列满时淘汰最旧事件。[C3] 这会产生重复提交或丢失关键事件的风险，需真实报文回归验证，而不是作为已发生事故陈述。

建议重试策略：

| 请求性质 | 默认策略 |
|---|---|
| 只读查询 | 有界退避；鉴权和确定性参数错误不盲目重试 |
| 创建 Run | 仅在目标服务支持且本次请求具有固定幂等键时重试 |
| Chat / Responses / Session Chat POST | 不因读取中断自动重新提交 |
| 订阅已有 Run | 可重新附着，但必须核对运行状态和事件连续性 |
| 审批、立即执行 Job、其他写操作 | 无已验证的幂等契约时不自动重放 |

应用层、SDK 和 OkHttp 内建重试必须作为整体检查。不能 SDK 禁止重放、底层却仍在允许语义不明确的恢复。凭据刷新也不能自动把结果未知的写请求送到另一身份下。

每个协议维护自己的终态识别规则，终态完成本地 Promise 至多一次。EOF 不等于成功：有可查询句柄时进行状态核对，否则返回 StreamInterrupted/UnknownOutcome，而不是拼接部分文本后报告完成。

事件 ID 或游标仅在服务端实际支持恢复时使用。重新连接不构成无损恢复承诺；不能用相同文本的 hash 去重，连续相同文本也可能是合法输出。发现缺口时标记不完整，并尽可能查询最终状态。

默认队列溢出策略建议为显式失败并保留观测信息。普通进度可由调用方显式选择合并；文本增量不能静默丢弃；审批、错误、终态必须进入可靠处理路径。容量耗尽时可以终止观察并返回明确异常，不能声称内存有限同时保证无限速事件不丢。

回调执行与共享网络线程隔离，同时保持每个订阅的事件顺序。重连次数、退避和执行器队列均有界。检查 SSE 的总调用超时与空闲检测，不只把 readTimeout 设置为零就视为长连接问题解决。

### 3.5 公开异步接口的取消传播

**待回归的代码路径：**当前底层 Future 上注册了取消回调，但公开结果又经 thenApply 转换。[C4] 验收必须从公开方法返回值发起取消，不能只测内部 Future。

设计一个贯穿请求链的 CallHandle / CancellationContext。SDK 直接返回的可取消句柄负责取消对应 HTTP Call、移除取消注册、释放其排队和重试任务。调用方自行派生的任意 CompletableFuture 不在自动反向传播承诺内；需提供共享取消令牌。

语义分为：取消本地等待、取消本地网络请求、停止远端 Run 三种动作。HTTP/SSE 关闭和观察超时不应隐式调用远端 stop。已经发出的写请求取消后，远端结果可能未知，应允许调用方核对，不能假装请求没有执行。

**验收：**取消公开 Future 后该 Call 被取消；取消未开始的重试不再发送请求；其他请求不受影响；观察超时不停止 Agent；关闭根对象清理自己拥有的资源。

## 4. 第二批：完整的 HTTP 运行闭环

### 4.1 统一请求上下文

新增三个相互独立的值对象，避免一个配置对象承担身份、业务和传输所有含义。

| 对象 | 建议内容 |
|---|---|
| `RequestContext` | ProfileBinding、sessionId、sessionKey、correlationId |
| `RequestOptions` | deadline、取消令牌、幂等键、允许的附加 headers |
| `ModelSelection` | model、provider、modelOptions、可扩展字段 |

请求发出前形成不可变快照。合并顺序为“本次请求显式值 > 客户端默认值”，但服务端自身的路由优先级仍由 Hermes 决定；SDK 不能承诺显式 model 一定成为最终模型。

sessionId 和长期记忆范围不得合为一个字段。平台根据授权后的租户/用户身份选择它们，普通用户不能通过自填 Header 获得另一身份。SDK 只验证、携带和隔离上下文，不自行推断租户权限。

### 4.2 Responses 流与输出聚合

新增独立的 Responses 流式入口和 StreamingResponseHandle，不能让 stream=true 的请求进入“读取完整响应体再解析 JSON”的路径。普通 JSON 方法遇到明确流式请求应拒绝并指引专用入口，避免长时间等待后得到误导性解析错误。

引入 ResponseEvent、OutputItem、ResponseAccumulator。累积器按 output/item/content 索引及服务端标识组织，而不是一律取第一个 choice 或拼所有 text。保留扩展字段与原始输出；最终答案、过程消息、工具活动、可展示的推理内容互不混合。

最终结果包含服务端终态、输出项、用量、实际运行信息、是否完整和必要的诊断引用。取消订阅不等于请求服务端删除或停止。异常结束时部分内容必须标记为部分内容。

新增回归用例覆盖：无正文的合法终态、交错工具/文本、未知事件、可选字段缺失、重复终态、断线缺口、回调异常、多索引输出。所有字段形状以固定的目标 Hermes 版本样本确定。

### 4.3 RunHandle 与状态核对

拟议入口：`submitRun` / `attachRun` 返回同一类 RunHandle；旧 create/get/stop 方法暂保留兼容。

```text
RunHandle
  runId()
  snapshotAsync()
  subscribe(listener)
  approve(decision)
  requestStop()
  awaitTerminal(observationDeadline)
  closeObservation()
```

以上方法名是职责示意，正式签名在 API 兼容检查后定稿。

必须区分**服务端状态**与**本地观察状态**。本地出现 DISCONNECTED、TIMED_OUT 或 CLOSED，不代表服务端 cancelled。服务端未知状态以 rawStatus 保留，不能硬映射到 success/failure。停止接口的应答只代表停止请求被接收，最终状态仍需事件或查询确认。[S1]

终态等待支持截止时间和有界轮询。HTTP 404 可能来自记录不可见、过期或确实不存在，不能被 SDK 自作主张转换为“执行成功”或泄露另一 Profile 是否拥有该 Run。

提交幂等键由调用方或可恢复的宿主工作流持有。SDK 为一次新操作生成键后，其所有重试必须使用同一键、同一请求快照和同一凭据身份范围。应用进程重启后的恢复需要宿主持久化，不能承诺靠 SDK 内存解决。

### 4.4 人工审批

新增 ApprovalRequest、ApprovalDecision、ApprovalReceipt 和 ApprovalHandler。具体 wire body 按目标服务端 schema 映射，不把 Java 字段名直接当作服务端字段。

默认无处理器时不自动同意。审批与 endpoint、Profile、Run、当前挑战身份绑定；服务端有 approvalId、版本或有效期时原样利用，没有时不伪造服务端保证。超时、断连、过期和重复提交都需明确结果；本地防重不宣称等价于服务端 exactly-once。

审批回调不能阻塞协议读线程。审批提交成功不代表运行成功，仍要等待实际运行终态。高权限的“始终允许”决定必须由宿主显式开放，不作为 SDK 默认值。

### 4.5 模型、会话、任务与能力发现

建议按同一模式新增较小的领域客户端；旧门面方法委托它们，不在首批移动所有类。

| 客户端 | 本轮目标 |
|---|---|
| Models | 提供 provider/model 选择元数据；请求选项与最终 runtime 分开 |
| Sessions | 类型化创建、更新、分叉、分页、历史和聊天请求 |
| Jobs | 类型化任务与增量修改，保留原始字段逃生口 |
| Capabilities | 能力快照、支持性判断、版本证据、缓存隔离 |
| Health | 区分存活、可用性、鉴权失败和传输失败 |

Jobs 的 PATCH 必须区分“不修改、赋值、显式清空”，不能全部依赖 Java null。时间、投递、技能和工作目录等选项只在目标契约确认后暴露为承诺能力。[S4] 对立即执行等写操作不凭方法名推断可重试。

能力查询失败时状态为 UNKNOWN，而不是全部支持或全部不存在。高层自动化操作对未知能力要求显式选择；原始高级入口可保留，但不隐式绕过鉴权或安全策略。能力响应中的 endpoint 字段也不能成为访问任意新 origin 的授权。

### 4.6 统一错误和响应元数据

新增 ApiResponse<T> 与 HermesError，保留 HTTP 状态、必要响应头、服务端错误码、请求关联信息和受长度限制的原始错误体。错误类别建议包括配置、认证、权限、限流、幂等冲突、协议解析、流中断、审批、本地进程、远端业务和结果未知。

是否重试由错误类别、操作语义、提交阶段和目标契约共同决定，不能按“所有 IOException 都重试”处理。Retry-After 只在返回且有效时使用，并受调用 deadline 限制。

新的删除 API 应区分删除成功、目标不存在、无权限和服务错误。旧 boolean 入口若需保持行为，则明确弃用说明，不把新 API 的严格错误语义悄悄抹掉。

## 5. 第三批：CLI 结构化流与进程管理

### 5.1 三种执行模型

现有执行器收集 stdout/stderr 到 ByteArrayOutputStream，并在进程退出后返回；这是有限命令执行路径，不是长连接协议实现。[C5] 官方 CLI 提供 JSONL 机器输出，因此不应从终端 UI 文本抓取结构化事件。[S2]

建议保持 execute 的有限命令语义，新增另外两种模型：

| API 职责 | 输入输出 | 适合场景 |
|---|---|---|
| 有限执行 `execute` | 一次输入、一次结果 | version/help、有限配置操作 |
| 流式执行 `executeStreaming` | 一次输入、连续 JSONL、最终结果 | 编程式 Agent 单轮任务 |
| 受管进程 `startManagedProcess` | 持续存活、可双向通信、显式关闭 | ACP、长期服务进程 |

异步包装 execute 并不能替代后两者。交互式 TUI、浏览器登录、配置向导不得假装在非交互 API 中完全受支持；需要交互终端的流程应显式委托宿主。

### 5.2 JSONL 解码与终态

新增 CliEvent、CliStreamDecoder、CliRunResult、HermesCliProcessHandle。stdout 与 stderr 分离，逐行处理完整 UTF-8 记录，保留跨 read 的不完整字节和不完整行。事件类型、字段和退出码采用目标 CLI 版本的契约。[S2]

终态结果与进程退出码要相互核对；若已经开始对话却没有有效终态记录，报告协议中断或结果未知。若进程在协议初始化前因缺失程序、认证或配置而退出，则返回明确的启动失败和受限 stderr，而不是一律报“缺失 result”。

文本分片不执行 trim；调试日志不能混入机器协议 stdout；未知事件保留、畸形事件分类处理。终态结果与此前增量可能有重复，累积器应采用最终权威结果或明确的聚合规则，不能双重拼接。

### 5.3 进程上下文与资源边界

CliExecutionOptions 建议承载 executable、工作目录、显式环境、Profile/Hermes Home、执行 deadline、最大 stdout/stderr、最大 JSONL 行、取消策略和进程所有权。

配置每次执行形成快照，不通过修改全局默认 Profile 处理并发请求。敏感环境变量按显式名单继承/覆盖，日志不输出完整环境。保留 argv 边界、不经 shell 拼接；需要 shell 的高级调用必须单独命名并明确风险。

输出缓冲设置硬上限。超限时默认返回 OutputLimitExceeded 并处理自己拥有的进程；允许调用方显式选择安全目录中的落盘方案，必须带限额、访问权限和清理策略，不能以无限磁盘替代无限内存。

进程并发上限应真正接入有界队列或信号量，并在 deadline 内可取消；资源从所有退出路径释放。进程取消顺序建议为协议内取消、宽限等待、操作系统终止、确认退出，且只作用于 SDK 拥有的进程。

Java 8 的公共 API 不依赖 java.lang.ProcessHandle。平台相关的进程树处理置于内部 ProcessSupervisor，分别验证 Linux/macOS/Windows；某个平台不支持的清理能力必须在报告中标记，不能宣称跨平台进程树清理已经完成。

### 5.4 CLI 命令与用量契约

新增机器可读用量结果，区分请求模型与实际运行模型、输入输出与缓存 token、进程时长与模型调用时长。无字段就保留空，不从人类日志猜测。

逐项核对真实 --help、官方文档和目标命令解析器；特别检查前序审计指出的 cronAdd/cron create 对齐问题。别名是否存在按版本确认，不根据方法名推断。[S4]

为配置、Profile、Skills、Jobs 等常用有限管理命令提供类型化选项；继续保留原始 argv 高级入口，但不在能力探测时运行 install、delete、update、gateway start 等有副作用命令。

## 6. 第四批：ACP 双向协议

官方 ACP 是双向 stdio JSON-RPC，其 stdout 是协议通道，权限请求也通过协议返回宿主。[S3] 仅启动 `hermes acp` 并等待退出不构成 Java ACP 客户端。

### 6.1 核心组件

| 组件 | 职责 |
|---|---|
| AcpStdioTransport | 持续 stdin/stdout、独立 stderr、消息帧边界 |
| JsonRpcDispatcher | 请求 ID、响应关联、通知分流、有界待处理请求 |
| AcpSessionClient | 初始化、能力协商、会话与 prompt 生命周期 |
| AcpPermissionHandler | 挂起与应答权限请求、超时处理 |
| AcpEventAdapter | 原生事件到统一观测模型的可追踪映射 |
| ProcessSupervisor | 进程启动、异常退出、关闭和资源清理 |

不能假设 ACP framing 与 CLI JSONL 完全相同。版本、帧边界和方法名称由选定 ACP 契约确定；需要时优先复用经过验证、满足目标 Java 基线的协议实现，再决定依赖或内部适配方式。

### 6.2 最小可用闭环

第一版 ACP 必须完成：启动并初始化、识别能力、创建会话、提交文本 prompt、消费通知、处理权限请求、取消当前轮、确认退出。加载/恢复/分叉/列出会话等按实际协商能力逐步开放。

权限默认不自动批准。未配置处理器、处理器异常或超时都应向调用方暴露并采用拒绝策略。高权限授权由宿主显式决定。权限回调在独立执行路径运行，避免阻塞读循环而造成相互等待。

JSON-RPC ID 与会话 ID、Run ID 分开。并行同名工具不得仅以工具名称关联完成事件；优先使用原生调用 ID，没有充分关联信息时明确标记不确定。

进程退出应使所有挂起请求结束为明确异常；重启后只有服务端承诺支持的会话恢复可以执行，不能自动重放刚才的 prompt。ACP 与 HTTP 的能力不会被伪装成完全等价，尤其是多模态、工具范围和认证流程。

### 6.3 依赖与发布约束

首批修复不依赖 ACP。ACP 可先保持独立包和显式启用；若最终协议依赖无法满足 Java 8，则将 ACP 发布为可选构件或提供不同适配实现，不能让基础 SDK 因 ACP 提升最低 JDK。正式拆模块须另行评审坐标和兼容成本。

## 7. 可选扩展，不阻塞基础修复

| 扩展 | 适用条件 | 设计边界 |
|---|---|---|
| A2A | 存在跨进程、跨机器或跨框架协作 | 独立认证与任务上下文，不能把 A2A TaskId 当 HTTP RunId |
| MCP Bridge | Java 需要调用 Hermes 暴露的 MCP，或配置 Hermes 的外部 MCP | 区分配置管理、协议客户端、权限三个方向 |
| Dashboard/Admin | 确有自动化管理需求 | 先确认公开契约与鉴权，避免依赖内部页面 RPC |
| Browser Controller | 确有宿主浏览器接管需求 | 独立能力协商、身份与操作许可；首期不实现 |
| Observer Hook Bridge | 需要额外 Python 内部观测 | 明确 Hermes 侧桥接组件与版本，不虚构远程订阅接口 |

A2A 与 MCP 的官方接入面彼此不同，所以它们应当是独立适配方向，而不是统一包装成 `chat()`。[S5][S6]

不将“兼容聊天接口”等同于支持全部媒体或文件 API。输入类型、工具执行方向与协议能力应逐项列在兼容表，不支持的内容明确报错。

## 8. 统一观测与数据安全

新增无依赖的 RuntimeObserver / RequestObserver 接口，允许业务系统订阅请求、订阅、审批、进程与状态核对的生命周期。OpenTelemetry、Micrometer 等通过可选适配接入，不成为核心强依赖。

建议指标包括请求耗时、活跃 Run 观察数、连接数、重连次数、队列利用率/溢出数、协议错误、CLI 进程数、审批等待和超时。标签避免直接放完整 sessionId、runId 等高基数数据；完整关联信息留在受控日志或 trace。

日志默认关闭正文，敏感头、令牌、Cookie、环境值及可识别秘密必须脱敏；即便开启 BODY 也受硬长度限制。原始协议报文的保留指内存值或经授权的诊断能力，不等于默认写入磁盘、遥测或第三方日志。

用量模型记录提供方、实际模型、输入输出/缓存 token 和字段来源。费用只能由宿主结合价格版本和计费规则估算；SDK 不把估算值冒充真实账单。重连和重复终态不应产生重复用量结算，宿主需用稳定执行身份防重。

## 9. 三分支协同维护

### 9.1 一套契约，三套兼容实现

三条版本线共享 JSON 字段、错误语义、取消语义、终态规则和测试样本；依赖导入、JSON mapper 适配以及 JDK 特有实现允许不同。

保持分支现有依赖代际，不把协议修复与 Jackson/OkHttp/构建插件大升级捆绑。默认可在 `feature/2.0.x` 先实现 Java 8 可表达的公共逻辑，随后立即同步到另外两条分支；交付批次必须三条同时闭环，不允许长期“主线已修、旧线以后再说”。这是建议流程，不表示该分支已成为仓库默认分支。

### 9.2 同步方式

按职责拆提交：先样本与契约测试，再共享逻辑，再分支适配，最后文档。共享提交优先 cherry-pick，适配提交独立标记。不要通过把整条长期分支 merge 进另一条来同步依赖矩阵。

API Shape Guard 比较新公共 API 的业务语义与签名；对历史存在的 Jackson 2/3 类型差异使用明确白名单，不要求所有构造器逐字相同。公共新增模型不得依赖 record、sealed、Flow 或虚拟线程。

Source Sync Guard 只比较声明为共享的文件或规范化后的逻辑，不粗暴要求所有文件 hash 相同。协议样本可以使用单一基准副本，以 manifest 校验三分支内容一致。

README 中的 JDK、构件版本和依赖矩阵由对应 POM 生成或校验。Maven wrapper、实际运行 JDK 和依赖字节码都进入 CI；不能只在高版本 JDK 编译 `--release 8` 就宣称实际 Java 8 运行通过。

### 9.3 兼容与迁移

| 旧入口 | 迁移策略 |
|---|---|
| HermesClient 构造器与常用方法 | 保留，内部委托 |
| SseEvent / StreamingChatResponse | 保留适配层，修正解析；记录行为变化 |
| forProfile(String) | 优先 resolver；缺失明确报错；新增显式凭据重载 |
| 通用 Map 返回 | 保留高级/raw 入口；新增类型化入口 |
| CLI execute | 保留有限执行语义，不悄悄变成长连接 |
| 测试 URL 旁路 | 弃用并从生产示例移除，不立即删除 |
| 旧重连行为 | 作为正确性变更明确发布；不默认保留危险 POST 重放 |

修复引入的安全行为变化必须写入迁移文档，并配最小前后示例。源代码兼容不等于业务行为零变化，不能在发布说明中宣称完全无感。

## 10. 实施任务清单与依赖

以下均为待实施任务。完成判定同时要求代码、测试和证据；没有执行就不得勾选。

### Phase 0：建立可复现基线

- [ ] **H-001** 固定三条源码提交，确认实际 JDK/POM/依赖要求；记录待兼容 Hermes 版本及 commit。
- [ ] **H-002** 建立能力—接口—协议—样本—测试映射表，区分 SUPPORTED / UNSUPPORTED / UNKNOWN。
- [ ] **H-003** 建立脱敏协议 fixtures 与 manifest；标明官方示例、真实捕获、人工边界样本的不同来源。
- [ ] **H-004** 在三条分支复现第一批风险；从公开 API 编写失败回归测试；保留结果。

**完成门槛：**每个修复有可复现失败或明确静态证据；目标服务端版本已确定；不能把尚未捕获的真实报文写成已采集。

### Phase 1：正确性修复

- [ ] **H-101** 实现 EndpointPolicy，接入 HTTP/SSE 与 DNS/redirect 约束，迁移本地示例。
- [ ] **H-102** 实现 ProfileBinding、CredentialProvider/Resolver、隔离缓存与轮换。
- [ ] **H-103** 实现 SseFrame 和端点解码器，分离解析/回调/传输错误。
- [ ] **H-104** 按请求语义拆重连与重试；识别协议终态；处理 EOF 与结果未知。
- [ ] **H-105** 实现有界有序事件派发及显式溢出处理；关键事件不静默丢弃。
- [ ] **H-106** 修复/验证公开取消链、关闭竞态、取消注册、重连调度与资源所有权。

**依赖：**H-103 先于 H-104/H-105；H-101/H-102 与事件解析可独立推进，最终通过 H-106 联合验证。

**完成门槛：**三分支第一批回归全过；命名 Profile 独立鉴权；一次聊天 POST 不因断线自动重复发送；根对象关闭不影响外部共享对象的无关任务。

### Phase 2：HTTP 运行闭环

- [ ] **H-201** 实现 RequestContext / RequestOptions / ModelSelection 的快照与映射。
- [ ] **H-202** 实现 Responses 流式入口、OutputItem 与严格的最终结果聚合。
- [ ] **H-203** 实现 RunHandle、幂等提交、断线状态核对和真实终态等待。
- [ ] **H-204** 实现 ApprovalHandler 及绑定、超时、过期、重复提交处理。
- [ ] **H-205** 补齐模型选择、会话、Jobs、能力与 Health 的类型化入口及必要 async 方法。
- [ ] **H-206** 实现统一错误、响应头保留、用量/runtime 模型和 observer 接口。

**依赖：**以 Phase 1 的身份、传输、取消语义为前提；H-201 支撑其余 HTTP 功能，H-203 支撑 H-204。

**完成门槛：**有一条真实服务上的创建→订阅→审批→停止→终态核对用例；没有把查询、取消或网络错误包装成 Agent 成功。

### Phase 3：CLI 集成

- [ ] **H-301** 实现 CliExecutionOptions、有界输出/排队与进程所有权。
- [ ] **H-302** 实现 JSONL 流式解码、stderr 分离和终态/退出码核对。
- [ ] **H-303** 实现取消、超时、平台进程清理与残留检测。
- [ ] **H-304** 实现结构化用量、常用管理子命令和逐版本 CLI 契约测试。

**依赖：**H-301 为后续基础；H-303 的平台能力是 ACP 进程管理的前置条件。

**完成门槛：**进程未退出前可观察增量；长输出内存有界；环境/Profile 不串用；每个声称支持的平台都有退出清理证据。

### Phase 4：ACP

- [ ] **H-401** 固定 ACP 契约和依赖策略，实现双向 transport、请求关联与初始化。
- [ ] **H-402** 实现会话、prompt、事件和能力协商；不支持能力明确拒绝。
- [ ] **H-403** 实现权限请求、取消与异常退出清理，验证读写不死锁。
- [ ] **H-404** 对选定 Hermes 版本完成互操作测试与迁移示例。

**完成门槛：**由 Java 完成至少一次真正双向会话和权限应答，而不是只拉起进程；其发布不影响基础 HTTP/CLI 用户。

### 发布与持续治理

- [ ] **H-501** 建立三分支契约/API Shape/Source Sync/真实 JDK 运行检查。
- [ ] **H-502** 完成连接、Profile、流、审批、取消和 CLI 压力/故障注入测试。
- [ ] **H-503** 更新 README、兼容矩阵、迁移说明、示例和发布证据清单。
- [ ] **H-504** 按批次发布候选版本与验证报告；A2A/MCP/Admin 单独立项，不阻塞修复版。

建议的发布顺序是 Phase 1 修复候选版、Phase 2 HTTP 完整版、Phase 3 CLI 增强版、Phase 4 ACP 可选版。每批单独验收，不等待所有扩展完成才交付已经验证的修复。

## 11. 验收测试矩阵

### 11.1 必须覆盖的场景

| 编号 | 场景 | 预期 |
|---|---|---|
| T-01 | 显式可信环回连接 | 不调用测试旁路即可成功 |
| T-02 | 不可信目标/跨 origin 跳转 | 发送凭据前拒绝 |
| T-03 | 两 Profile 两密钥并发 | 认证、缓存、上下文不串用 |
| T-04 | 凭据轮换 | 后续请求使用新绑定，不重放结果未知写操作 |
| T-05 | 标准 Chat chunk | 文本正确，不要求 JSON 再包一层 data |
| T-06 | Responses 交错输出 | 工具、过程消息与最终答案不混合 |
| T-07 | Session 正常终态后 EOF | 完成一次，聊天 POST 次数保持一次 |
| T-08 | Session POST 中途断线 | 不自动重复执行；暴露中断或结果未知 |
| T-09 | Run 重新附着 | 状态核对；不能无依据宣称事件连续 |
| T-10 | 队列饱和 | 明确溢出结果；不静默丢审批或终态 |
| T-11 | 回调抛异常 | 不伪装为 JSON 解析失败，资源仍释放 |
| T-12 | 请求发出后丢失提交响应 | 有幂等契约时复用同键核对，不另建操作 |
| T-13 | 幂等键与载荷冲突 | 暴露冲突，不换新键悄悄重新执行 |
| T-14 | 停止应答已收到 | 继续核对终态，不提前宣告 worker 已退出 |
| T-15 | 审批拒绝/超时/过期 | 默认不执行被限制动作，错误可观察 |
| T-16 | 取消公开异步结果 | 对应 Call 取消，其余调用不受影响 |
| T-17 | 仅关闭观察句柄 | 远端 Run 不被隐式停止 |
| T-18 | Future 用户派生阶段 | 文档明确取消令牌边界，不给虚假反向传播保证 |
| T-19 | Java 8/17/21 同报文 | 公共业务结果一致，差异均有明确白名单 |
| T-20 | CLI UTF-8 分片/长行 | 正确恢复记录；超限明确失败 |
| T-21 | CLI 启动前失败 | 启动错误、退出码和有限 stderr 可用 |
| T-22 | CLI 已开始后终态缺失 | 标记协议中断，不按部分文本成功返回 |
| T-23 | CLI cancel/timeout | 对拥有的进程按平台能力清理并验证 |
| T-24 | ACP 请求与权限双向交错 | 请求关联正确、协议读循环不死锁 |
| T-25 | ACP 进程退出 | 挂起请求全部结束；不自动重放 prompt |
| T-26 | 未知能力/事件/状态 | 保留信息并明确不确定性，不当作成功 |
| T-27 | 外部注入共享客户端 | SDK close 不停止无关调用或全局资源 |
| T-28 | 日志与诊断 | 默认不含秘密和未授权正文；原始报文也受控 |

### 11.2 测试层次与证据

使用四层验证：纯模型/状态机测试、真实传输的 MockWebServer/伪 CLI 测试、固定 Hermes 版本的互操作测试、隔离环境的压力与故障注入测试。

模拟协议服务要使用经来源标记的 fixtures，不能只按 SDK 实现生成迎合式报文。真实 Agent 测试限制工具为无副作用或审批控制的测试工具，不能为了 CI 方便开启全局自动批准。涉及模型调用的成本、凭据和并发上限由测试环境显式配置。

压力测试可沿用 100/300/500/800/1000 档位评估 HTTP/SSE 传输，但使用可控模拟服务，不默认启动上千个真实模型/Agent。重点记录内存上限、线程/连接/进程数、队列深度、p50/p95/p99 延迟、错误类型和回收结果。

性能阈值在 Phase 0 基线测量后按固定机器与工作负载冻结；本方案没有未经测量的吞吐承诺。可接受指标必须同时考虑客户端 CPU、内存和事件完整性，不能以静默丢事件换取更高吞吐。

### 11.3 证据产物建议

```text
verification/
  source-manifest.json
  server-compatibility.json
  fixtures-manifest.json
  public-api-shape.json
  branch-sync-report.json
  unit-and-transport-tests/
  hermes-contract-tests/
  resource-lifecycle-tests/
  benchmark-summary.json
```

这些是拟议产物路径，不是本次已经生成的测试结果。所有报告应包含源码 commit、Hermes commit/version、实际 JDK、OS、依赖锁定信息、执行命令和时间。日志及样本先脱敏，不把凭据写入仓库。

## 12. 文件级改动建议

以下相对路径以 `src/main/java/io/github/easy4j/hermes/` 为根；新增名称可在实现评审中调整，但职责边界不得合并回一个巨大门面。

| 现有文件/区域 | 拟议改动 |
|---|---|
| HermesHttpClientConfig.java | 引入端点/默认请求策略，减少可变共享配置，弃用测试旁路 |
| util/EndpointGuard.java | 变为策略校验辅助，连接期与配置期规则一致 |
| HermesClient.java | 保留兼容门面；Profile resolver；委托领域客户端；明示所有权 |
| HermesOkHttpClientFactory.java | 验证传输策略、连接/调度资源所有权及重试行为 |
| api/HermesHttpClient.java | 统一执行上下文、公开取消链、响应元数据和错误 |
| api/HermesSseClient.java | 原始帧、端点解码、终态、重连与回调隔离 |
| api/sse/SseEvent.java | 旧兼容视图与新原始帧分离；保留未知载荷 |
| api/sse/StreamingChatResponse.java | 明确取消/部分输出/终态语义，防止二次累计 |
| api/model/*Request.java | 按契约增加模型选择和上下文快照，不删旧字段 |
| api/model/*Result.java | 类型化输出、runtime/usage 与扩展保留 |
| cli/HermesCliExecutor.java | 有限执行兼容委托，输出/并发有界，不承担 ACP 协议 |
| cli/HermesCli.java | 结构化流与命令选项入口，保留原始 argv |
| 新增 context/security/transport/runtime | 请求、凭据、调用句柄、Run 与审批 |
| 新增 cli/stream 与 cli/process | JSONL、受管进程、平台能力 |
| 新增 acp | 后续独立协议，不阻塞基础发布 |
| src/test 与 scripts | 真实形状样本、三分支同步、资源/兼容与压力证据 |

## 13. 与上层 Agent 平台的集成建议

SDK 保持独立，不直接依赖你的 Agent Fabric 协议。平台侧建立 HermesRuntimeAdapter，将 SDK 事件映射为平台自己的运行事件和 trace；Profile、租户与凭据授权由平台提供。

对 Agent-Job 与 Hermes Cron 的调度权必须二选一：由 Agent-Job 调度时调用 Hermes 的单次执行入口；委托 Hermes Cron 时记录外部 Job 标识并监控其状态，不能双方都登记同一个定时任务造成双重执行。

Agent-Buddy 的本地可视化消费 CLI/ACP 的结构化事件，云端或私有化 Runtime 消费 HTTP/Run 事件。不同协议仍保留来源及能力差异，不由适配器虚构同等的恢复、权限或工具能力。

企业审计与费用结算由平台持久化；SDK 提供事实性运行和用量信息。不能因为 SDK 能调用 stop/approval，就让任何前端用户绕过平台授权直接调用。

## 14. 发布门槛与首个实施批次

首个实施批次固定为 H-001～H-004、H-101～H-106，并同步引入必要的 H-501 检查。必须先写出重现标准 SSE 解析和 POST 重放风险的回归，再修改传输代码。

这一批不增加 A2A、不实现完整 Dashboard、不拆全仓模块，也不升级全部依赖。发布候选版时说明哪些风险已由真实测试证实/消除，哪些仍只有静态审计依据。

完成定义：三条分支测试证据齐全；本地端点无需测试旁路；Profile 凭据不串用；正确解析目标协议报文；断线不盲目重提 Agent 工作；停止与取消语义明确；线程、连接、Future 和受管进程可解释地收敛；文档与公开 API 行为一致。

**执行主线：先修正确性，再补运行闭环；先守住安全和身份，再扩展协议；先证明真实互操作，再扩大覆盖面。**

## 附录 A：源码与官方资料

以下来源用于事实核对，设计决策与任务拆分由本方案提出。源码链接固定审计 commit；官方文档为 2026-09-20 查阅时的网页内容，实施时须再固定目标 Hermes 版本，不把网页更新等同于已部署服务支持。

- [C0] 分支集合：<https://api.github.com/repos/easy-4-java/hermes-java-sdk/branches?per_page=100>
- [C1] EndpointGuard：<https://github.com/easy-4-java/hermes-java-sdk/blob/3b3f148a09987442c71586cec809a62529f4c940/src/main/java/io/github/easy4j/hermes/util/EndpointGuard.java>
- [C2] HermesClient：<https://github.com/easy-4-java/hermes-java-sdk/blob/3b3f148a09987442c71586cec809a62529f4c940/src/main/java/io/github/easy4j/hermes/HermesClient.java>
- [C3] HermesSseClient：<https://github.com/easy-4-java/hermes-java-sdk/blob/3b3f148a09987442c71586cec809a62529f4c940/src/main/java/io/github/easy4j/hermes/api/HermesSseClient.java>
- [C4] HermesHttpClient：<https://github.com/easy-4-java/hermes-java-sdk/blob/3b3f148a09987442c71586cec809a62529f4c940/src/main/java/io/github/easy4j/hermes/api/HermesHttpClient.java>
- [C5] HermesCliExecutor：<https://github.com/easy-4-java/hermes-java-sdk/blob/3b3f148a09987442c71586cec809a62529f4c940/src/main/java/io/github/easy4j/hermes/cli/HermesCliExecutor.java>
- [S1] API Server：<https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server>
- [S2] CLI Commands Reference：<https://hermes-agent.nousresearch.com/docs/reference/cli-commands>
- [S3] ACP Internals：<https://hermes-agent.nousresearch.com/docs/developer-guide/acp-internals>
- [S4] Scheduled Tasks / Cron：<https://hermes-agent.nousresearch.com/docs/user-guide/features/cron>
- [S5] A2A：<https://hermes-agent.nousresearch.com/docs/user-guide/messaging/a2a>
- [S6] MCP：<https://hermes-agent.nousresearch.com/docs/user-guide/features/mcp>
- [S7] Profiles：<https://hermes-agent.nousresearch.com/docs/user-guide/profiles>
- 官方全文入口：<https://hermes-agent.nousresearch.com/docs/llms-full.txt>（本次超出单次网页读取上限，采用对应章节核对。）