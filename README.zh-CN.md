# hermes-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-17-orange)](https://github.com/easy-4-java/hermes-java-sdk) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

[Hermes Agent](https://github.com/easy-4-java/hermes) 的纯 Java SDK — HTTP API Server + SSE 流式 + 本地 CLI 集成。无 Spring 依赖，JDK 8 基线。

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 功能与状态](#2-features--status)
- [3. 环境要求与兼容性](#3-requirements--compatibility)
- [4. 架构与模块](#4-architecture--modules)
- [5. 安装](#5-installation)
- [6. 快速开始](#6-quick-start)
- [7. 配置](#7-configuration)
- [8. 核心用法 / API](#8-core-usage--api)
- [9. 测试与构建](#9-testing--build)
- [10. 版本线与分支](#10-versioning--branches)
- [11. 参与贡献与许可协议](#11-contributing--license)

## 1. 项目概览

`hermes-java-sdk` 是集成 Hermes Agent（提供 HTTP API Server 的智能体服务）的纯 Java SDK（无 Spring 依赖）。SDK 在统一门面之后提供三个独立通信通道：

| 通道 | 说明 | 入口类 |
|:---|:---|:---|
| **HTTP** | 健康检查、聊天补全、Responses、Runs、Sessions、Models、Capabilities、Skills、Toolsets、Jobs 的 REST API | `HermesHttpClient` |
| **SSE** | Server-Sent Events 流式（聊天、运行事件、会话流） | `HermesSseClient` |
| **CLI** | 本地 `hermes` 命令执行与结构化输出解析 | `HermesCli` |

| 是什么 | 不是什么 |
|:---|:---|
| Hermes Agent 服务器的统一客户端 | Spring Boot Starter（不含自动配置） |
| Agent API 的类型化请求/响应模型 | Agent 实现本身 |
| 本地 CLI 封装 + 启动探测 | UI |

典型使用场景：

| 场景 | 说明 |
|:---|:---|
| 接收流量前的健康检查 | `health()`、`healthDetailed()`、`healthV1()` |
| 聊天补全 | `chatCompletion(ChatRequest)`、`chatCompletionWithSession(...)` |
| 流式聊天 | `chatCompletionStream(ChatRequest)` -> `ChatStreamingResponse` |
| Agent 运行 | `createRun(RunCreateRequest)`、`getRun`、`stopRun`、`approveRun` |
| 会话 | `createSession(title)`、`listSessions()`、`sessionChat(sessionId, input)`、`forkSession`、`deleteSession` |
| Responses API | `createResponse(ResponseRequest)`、`getResponse`、`deleteResponse` |
| 本地 CLI | `cli().chatOneShot(query)`、`cli().worktreeOneShot(query)`、`cli().version()` |
| Jobs | `listJobs()`、`createJob`、`getJob`、`updateJob`、`pauseJob`、`resumeJob`、`runJobNow` |

**项目状态：** 活跃开发。

<a id="2-features--status"></a>
## 2. 功能与状态

| 能力 | 状态 | 说明 |
|:---|:---|:---|
| 统一 `HermesClient` 门面 | 可用 | HTTP + SSE + CLI 三通道单一入口（`AutoCloseable`） |
| 聊天补全（同步） | 可用 | `chatCompletion(ChatRequest[, headers])`，带会话变体 |
| SSE 流式 | 可用 | `chatCompletionStream(ChatRequest)`、`sse().subscribeChat(...)`、`subscribeQueue(runId)` |
| Runs | 可用 | `createRun` / `getRun` / `stopRun` / `approveRun` |
| Sessions | 可用 | 创建 / 列表 / 详情 / 消息 / fork / 删除 / `sessionChat` |
| Responses API | 可用 | `createResponse` / `getResponse` / `deleteResponse` |
| Models 与 capabilities | 可用 | `listModels()`、`getModel(modelId)`、`getCapabilities()`、`listSkills()`、`listToolsets()` |
| Jobs | 可用 | 列表 / 创建 / 详情 / 更新 / 删除 / 暂停 / 恢复 / 立即运行 |
| 本地 CLI | 可用 | `HermesCli` + `HermesCliExecutor.execute(...)`、`probe()` |
| 启动探测 | 可用 | HTTP 与 CLI 可用性检查；`failFastOnUnavailable` 开关 |
| 健康端点 | 可用 | `/health`、`/health/detailed`、`/v1/health` |
| 集成测试 | 可用 | `HermesClientIntegrationTest`（测试源码） |
| CI 流水线 | 未配置 | 仓库中无 CI 工作流文件 |

<a id="3-requirements--compatibility"></a>
## 3. 环境要求与兼容性

| 依赖项 | 版本 |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| Hermes Agent 服务 | 运行中的实例（默认 `http://localhost:8642`） |
| OkHttp | 4.12.0 |
| Jackson | 2.22.0（`jackson-databind`） |

### 版本线矩阵

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

### 服务端 API 端点（`HermesApiConstants` 声明）

| 路径 | 用途 |
|:---|:---|
| `/health`、`/health/detailed`、`/v1/health` | 健康检查 |
| `/v1/chat/completions` | 聊天补全 |
| `/v1/responses` | Responses API |
| `/v1/models`、`/v1/capabilities`、`/v1/skills`、`/v1/toolsets` | 模型 / 能力 / 技能 / 工具集 |
| `/v1/runs` | Agent 运行 |
| `/api/sessions` | 会话 |
| `/api/jobs` | 任务 |

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
  业务代码                    hermes-java-sdk                   Hermes Agent
  --------                    --------------                   ------------
  请求模型  ->  HermesClient（门面）
                          |         |          \
                          |         |           \
                    HermesHttpClient   HermesSseClient      HermesCli
                    （OkHttp 3 +        （SSE 事件，           （本地 `hermes`
                      Jackson）         队列/消费者）          子进程）
                          |                |                    |
                          +----- HTTP -----+                    +---- 子进程
                                |                                       |
                    /v1/* REST 端点                          本地 CLI
                    （serverUrl 默认                          （executable
                     http://localhost:8642）                  `hermes`）
```

单一模块，jar 打包：

| 包 | 职责 |
|:---|:---|
| `io.github.easy4j.hermes` | `HermesClient`、配置（`HermesClientConfig`、`HermesHttpClientConfig`、`HermesCliConfig`） |
| `io.github.easy4j.hermes.api` | `HermesHttpClient`、`HermesSseClient`、`HermesApiConstants` |
| `io.github.easy4j.hermes.api.model` | 类型化模型（`ChatRequest`、`ChatResponse`、`Session`、`RunStatus`、`HealthStatus`、`SseEvent` 等） |
| `io.github.easy4j.hermes.cli` | `HermesCli`、`HermesCliExecutor`、`HermesCliResult` |
| `io.github.easy4j.hermes.cli.availability` | CLI 启动探测 |
| `io.github.easy4j.hermes.exception` | 类型化异常（`HermesException`、`HermesHttpException`、`HermesCliStartupException`） |
| `io.github.easy4j.hermes.util` | `HermesObjectMapper`、`HermesJsonParser` |

<a id="5-installation"></a>
## 5. 安装

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>hermes-java-sdk</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:hermes-java-sdk:2.0.x.x.20260630-SNAPSHOT'
```

**可用性：** 构件发布至阿里云私有 Maven 仓库，并通过 GitHub Releases 分发；尚未发布到 Maven Central。

<a id="6-quick-start"></a>
## 6. 快速开始

```java
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.ChatResponse;
import io.github.easy4j.hermes.api.model.HealthStatus;
import io.github.easy4j.hermes.api.model.Message;

HermesClientConfig config = new HermesClientConfig();
config.getHttp().setServerUrl("http://localhost:8642");   // Hermes Agent 服务
config.getHttp().setApiKey("optional-api-key");           // 可选

try (HermesClient client = new HermesClient(config)) {

    HealthStatus health = client.health();
    System.out.println("status=" + health.getStatus());

    Message message = new Message();
    message.setRole("user");
    message.setContent("Hello, Hermes!");

    ChatRequest request = new ChatRequest();
    request.setModel("hermes-agent");
    request.setMessages(java.util.Collections.singletonList(message));

    ChatResponse response = client.chatCompletion(request);
    System.out.println(response.getChoices().get(0).getMessage().getContent());
}
```

预期结果：打印健康状态，并从类型化 `ChatResponse` 打印助手回复内容。

<a id="7-configuration"></a>
## 7. 配置

配置集中在 `HermesClientConfig`，其包含 `HermesHttpClientConfig`（`getHttp()`）与 `HermesCliConfig`（`getCli()`）。

`HermesHttpClientConfig`：

| 属性 | 默认值 | 说明 |
|:---|:---|:---|
| `enabled` | `true` | 启用 HTTP 通道 |
| `startupCheckEnabled` | `false` | HTTP 通道启动探测 |
| `failFastOnUnavailable` | `false` | 探测不可用时快速失败 |
| `serverUrl` | `http://localhost:8642` | Hermes Agent 服务基础地址 |
| `apiKey` | — | 可选 API Key 请求头 |
| `connectTimeoutMillis` | `15000` | 连接超时 |
| `readTimeoutMillis` | `300000` | 读取超时 |
| `verifySsl` | `true` | TLS 校验 |
| `defaultModel` | `hermes-agent` | 默认模型名 |
| `defaultInstructions` / `defaultProvider` | — | 可选默认值 |

`HermesCliConfig`：

| 属性 | 默认值 | 说明 |
|:---|:---|:---|
| `enabled` | `true` | 启用 CLI 通道 |
| `startupCheckEnabled` | `false` | CLI 启动探测 |
| `failFastOnUnavailable` | `false` | CLI 不可用时快速失败 |
| `executable` | `hermes` | 本地 `hermes` 可执行文件名或路径 |
| `timeout` | `300` | 命令超时（秒） |
| `probeTimeoutSeconds` | `5` | 探测超时（秒） |
| `workingDirectory` | — | 工作目录 |
| `maxConcurrentExecutions` | `0` | 子进程最大并发数（`<=0`：`max(CPU 核心数, 2)`） |

<a id="8-core-usage--api"></a>
## 8. 核心用法 / API

### 8.1 会话内聊天

```java
Session session = client.createSession("My Session");
ChatResponse reply = client.sessionChat(session.getId(), "Tell me a joke");
```

### 8.2 创建并监控运行

```java
RunCreateRequest runRequest = new RunCreateRequest();
runRequest.setSessionId(session.getId());
runRequest.setInput("Analyze this data...");

RunStatus run = client.createRun(runRequest);
String runId = run.getId();
RunStatus current = client.getRun(runId);
client.stopRun(runId);
```

### 8.3 SSE 流式

```java
ChatStreamingResponse stream = client.chatCompletionStream(request);
// 或直接访问：
client.sse().subscribeChat(request, sseEvent -> {
    System.out.println(sseEvent.getType() + ": " + sseEvent.getData());
});
```

### 8.4 本地 CLI

```java
HermesCliResult oneShot = client.cli().chatOneShot("hello");  // hermes -z hello
HermesCliResult version = client.cli().version();             // hermes --version
```

### 8.5 HTTP 与 CLI 通道选择

`HermesClient.isHttpEnabled()` / `isCliEnabled()` 反映各通道是否启用。两者均启用时，`chatCompletion` 等高层方法走 HTTP 通道；CLI 调用通过 `cli()` 显式进行。

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
./mvnw clean verify        # 编译、运行测试、生成覆盖率报告
./mvnw clean install       # 安装到本地仓库
```

- 测试源码中包含集成测试（`HermesClientIntegrationTest`）。
- 覆盖率由 JaCoCo Maven 插件度量（目标：90% 行覆盖率，`haltOnFailure=false`）。
- `release` profile 组装 GPG 签名 + 源码 + Javadoc + 部署（`./mvnw -Prelease clean deploy`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

仓库维护三条并行版本线：

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

维护策略：在 JDK 8 作为基线的同时，1.0.x 版本线接收缺陷修复；新功能开发主要面向 2.0.x / 3.0.x 版本线。

<a id="11-contributing--license"></a>
## 11. 参与贡献与许可协议

欢迎参与贡献——请通过 Issue 反馈问题，或向对应版本线分支提交 Pull Request（JDK 17 相关改动提交到 `feature/2.0.x`）。

本项目基于 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可发布。详见仓库根目录的 `LICENSE` 文件。
