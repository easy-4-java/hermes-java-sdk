# 传输、安全与兼容性设计

## Context

动机与行为变化见 proposal.md。已观察三分支 HEAD 与静态风险见 docs/openspec/source-manifest.json 和原方案 §3；没有在本次执行 Maven 或真实 Hermes 回归。既有 OkHttp/Jackson/Commons Exec 代际继续维持，不做全仓重写。

## Goals / Non-Goals

目标：统一端点、身份、订阅、重试、取消和所有权的不变量；把风险用公开入口测试固定。非目标：Responses 完整产品 API、ACP、A2A、分布式调度、真实业务权限引擎及大规模依赖升级。

## Decisions

### 1. 显式策略而非全局放开私网

新增 EndpointPolicy 与 TargetOrigin；公共模式为 TRUSTED_LOCAL、TRUSTED_PRIVATE、PUBLIC_HTTPS。可信策略只由宿主配置，不接受终端用户直接改写。EndpointGuard 变成校验辅助，HTTP/SSE 统一调用。

配置阶段固定 scheme/host/port；使用 HttpUrl 路径段 API 编码标识。连接期通过策略包装的 DNS 校验全部候选地址，不在检查后交给另一个不受控解析器。默认关闭自动跨 origin 跳转；HTTP/HTTPS 降级拒绝。已知云元数据目的地禁止被宽泛私网规则意外覆盖；有特殊网络需求时用明确目标授权和部署出口策略。

替代方案：删除私网校验会扩大攻击面；只在 setter 校验无法约束重定向和解析变化，因此不采用。可信代理的远端 DNS 不由客户端虚假担保，需声明代理负责的范围。

### 2. 身份与连接池分离

ProfileBinding 固定 endpointIdentity、profileId、credentialIdentity；CredentialProvider 按请求解析密钥并提供可选 generation。缓存使用非秘密身份键；轮换使身份相关缓存失效。共享 ConnectionPool 不意味着共享认证 interceptor、CookieJar 或内容缓存。

旧 forProfile(String) 委托 ProfileCredentialResolver；无解析器或缺失命名凭据时失败。新增显式绑定入口。普通 headers 经过保护头检查，不允许覆盖 Authorization、Host 和身份 Cookie。外部 OkHttpClient 的有状态 interceptor 不能靠反射猜安全；多 Profile 路径要求已声明的隔离适配，否则拒绝派生。

### 3. SSE 三层管道

底层 EventSource 负责 SSE framing；监听器接收完整 id/type/data 后构建 SseFrame，不把 data 直接反序列化为含 data 字段的旧模型。端点 decoder 产出类型化事件，旧 SseEvent 通过兼容适配把 rawData 放入其可解析字段。

终态解析属于 decoder，不硬编码所有协议都使用 [DONE]。业务回调在有界、按订阅串行的派发器执行。ParserError、TransportError、ConsumerError 和 RemoteError 使用独立路径，finally 收敛资源。对不能更改帧读取上限的底层实现，必须确认底层最大消息保护或采用受测适配；不能只在巨大字符串已分配后才声称内存严格有界。

### 4. 提交策略与重新观察策略分离

RequestSemantics 标识 READ、AGENT_CREATE、CONTROL_WRITE、OBSERVE。Chat/Responses/Session POST 默认禁止响应读取中断后的重发。允许幂等重试时保存同一 payload 字节快照、key 和身份。审查 OkHttp 自动恢复与上层策略，不能仅关闭应用层重试。

订阅状态：NEW → CONNECTING → OPEN → REATTACHING 或 VERIFYING → TERMINAL / INTERRUPTED；CLOSED 表示本地关闭，不代表远端终态。CAS 保证最终完成至多一次。EOF 且无终态时，有句柄则核对原运行；否则返回 StreamInterrupted/UnknownOutcome。不得自动 fallback 到 CLI。

事件序号只用真实服务端 ID 去重；没有恢复契约就标记 continuity=UNVERIFIED。溢出默认 FAIL；进度合并显式选择；控制事件进入有界独立控制路径或直接使观察失败，不能无限排队来承诺永不丢失。

### 5. 显式取消和资源账本

CancellationContext 贯穿请求创建、映射后的公开 Future、队列及重试。不能只在原始 Future 注册 cancel 监听；SDK 返回的最外层结果必须绑定同一上下文。用户自行 thenApply 的 Future 通过共享令牌控制，不承诺标准 CompletableFuture 自动反向取消。

TransportOwnership 记录 HTTP client、dispatcher、pool、scheduler、subscription 是否 SDK_OWNED 或 EXTERNAL。内部派生资源由 SDK 关闭；外部客户端不能 cancelAll 或 shutdown 全局 executor。创建与 close 通过原子状态/锁保证新资源要么被拒绝，要么进入账本再被清理。close 幂等。

## Proposed public contracts

下列为拟议签名，分别存于对应 .java 文件；不表示当前可编译使用。

```java
public interface CredentialProvider {
    CredentialSnapshot resolve(ProfileIdentity identity);
}
public interface ProfileCredentialResolver {
    ProfileBinding resolve(String profileId);
}
public interface CancellationToken {
    boolean isCancellationRequested();
    AutoCloseable onCancel(Runnable action);
}
public interface StreamHandle<T> extends AutoCloseable {
    CompletableFuture<T> completion();
    void cancelObservation();
    void close();
}
```

值对象：ProfileIdentity 为非秘密端点/Profile/凭据身份；CredentialSnapshot 为秘密 token 与可选 generation，toString 永不输出 token；ProfileBinding 组合身份与 provider。SseFrame 含可空 eventId、eventName、rawData、receivedAtEpochMillis；EventEnvelope 含来源协议、原生类型、可用 run/session/callId、观察序号和 raw payload。所有可变输入做快照；敏感载荷不自动进入日志。

## File changes

| 动作 | 精确路径或目录 | 职责 |
|---|---|---|
| 修改 | src/main/java/io/github/easy4j/hermes/HermesHttpClientConfig.java | 正式端点策略与配置迁移 |
| 修改 | src/main/java/io/github/easy4j/hermes/util/EndpointGuard.java | 策略辅助 |
| 修改 | src/main/java/io/github/easy4j/hermes/HermesClient.java | Profile 绑定和关闭 |
| 修改 | src/main/java/io/github/easy4j/hermes/HermesOkHttpClientFactory.java | 策略、DNS、所有权 |
| 修改 | src/main/java/io/github/easy4j/hermes/api/HermesHttpClient.java | 公开取消链及请求语义 |
| 修改 | src/main/java/io/github/easy4j/hermes/api/HermesSseClient.java | framing/decoder/状态机委托 |
| 修改 | src/main/java/io/github/easy4j/hermes/api/sse/SseEvent.java | 旧事件兼容视图 |
| 修改 | src/main/java/io/github/easy4j/hermes/api/sse/StreamingChatResponse.java | 部分结果和终态 |
| 新增 | src/main/java/io/github/easy4j/hermes/security/EndpointPolicy.java | 显式信任策略 |
| 新增 | src/main/java/io/github/easy4j/hermes/security/ProfileBinding.java | 身份范围 |
| 新增 | src/main/java/io/github/easy4j/hermes/security/CredentialProvider.java | 请求级凭据 |
| 新增 | src/main/java/io/github/easy4j/hermes/transport/CancellationContext.java | 取消所有者 |
| 新增 | src/main/java/io/github/easy4j/hermes/transport/TransportOwnership.java | 资源账本 |
| 新增 | src/main/java/io/github/easy4j/hermes/api/sse/SseFrame.java | 原始帧 |
| 新增 | src/main/java/io/github/easy4j/hermes/api/sse/EndpointEventDecoder.java | 解码边界 |
| 新增 | src/main/java/io/github/easy4j/hermes/api/sse/OrderedEventDispatcher.java | 有界派发 |

测试精确文件和 Scenario ID 见 tasks.md 与 test-catalog.md。

## Risks / Trade-offs

外部 interceptor 有身份状态 → 不尝试无证据安全克隆；要求隔离或拒绝。自动重放关闭导致旧调用更常看见中断 → 提供原运行附着/核对，而非恢复危险行为。有界回调可能显式失败 → 交付可观察溢出和容量指标，不丢数据换吞吐。DNS/代理能力差异 → 支持矩阵区分本地可验证与可信出口承担的保证。

## Migration Plan

先加红色公开回归，再引入兼容委托。旧测试旁路标弃用并更新生产示例；Profile 显式绑定凭据。每个小批同步三个分支，针对接口和行为检查迁移示例。完整 close/取消/重连故障测试过后发布候选。

回滚只撤销尚未承诺的新入口或固定到已验证的安全版本；不得通过重新启用盲目 POST 重放或根凭据借用来回滚。客户端配置迁移可逐应用实施，不执行服务端破坏性迁移。

## Evidence gate

目标 Hermes commit、官方终态形状、幂等能力及代理路径在 H-001～H-004 记录。不具备证据的功能在兼容表标 UNKNOWN，测试任务不得勾选。这里不预先生成真实运行报告。

## Shared implementation constraints

实现依赖必须满足本项目总计划的顺序。三条版本线分别用 Java 8、17、21 执行，不把高版本 --release 编译当作低版本真实运行。保留 Maven 坐标与常用门面，新增公开类型不暴露 Jackson 2/3 特有类型；内部序列化适配允许分支不同。所有新名称均是待实现的目标接口，不是已存在 API。

来源和目标协议必须区分：前序审计固定了 SDK commit；目标 Hermes commit 尚未选择，先通过 tasks 中的采证门禁固定版本、抓取脱敏真实样本并确认 wire 映射。行为规范规定遇到未知能力如何处理，不为未确认协议发明字段。测试数据分为官方示例、真实捕获和人工边界三类。