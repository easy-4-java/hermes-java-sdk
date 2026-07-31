# hermes-java-sdk

> Hermes Agent 纯 Java SDK — HTTP API Server + SSE 流式 + 本地 CLI 集成。

[English](README.md) | [中文](README.zh_CN.md)

## 概述

`hermes-java-sdk` 是一个纯 Java SDK（无 Spring 依赖），用于集成 [Hermes Agent](https://github.com/easy-4-java/hermes)。它提供三个独立的通信通道：

| 通道 | 说明 | 入口类 |
|------|------|--------|
| **HTTP** | REST API（Runs、Chat Completions、Sessions、Models） | `HermesHttpClient` |
| **SSE** | Server-Sent Events 流式传输 | `HermesSseClient` |
| **CLI** | 本地 `hermes` 命令执行 | `HermesCli` |

Spring Boot 应用请使用 [hermes-spring-boot-starter](../hermes-spring-boot-starter)。

## 功能特性

- **统一客户端门面** — `HermesClient` 提供三个通道的单一入口
- **Chat Completions** — 同步聊天补全 API
- **Runs** — 创建、查询、停止 Agent 运行
- **Sessions** — 创建、列出、获取、删除会话；在会话中聊天
- **Models & Capabilities** — 列出可用模型和系统能力
- **SSE 流式** — 订阅实时 Server-Sent Events
- **本地 CLI** — 执行本地 `hermes` 命令并解析结构化输出
- **健康检查** — 服务器健康端点集成
- **启动探测** — 可配置的 HTTP 和 CLI 启动可用性检查

## 环境要求

- Java 8+（1.0.x 使用 JDK 8，2.0.x+ 使用 JDK 17）
- Hermes Agent 服务运行中（默认：`http://localhost:8642`）

## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>hermes-java-sdk</artifactId>
    <version>${hermes.version}</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:hermes-java-sdk:${hermes.version}'
```

## 快速开始

### 1. 创建配置

```java
import io.github.hiwepy.hermes.HermesClientConfig;

HermesClientConfig config = new HermesClientConfig();
config.setServerUrl("http://localhost:8642");
config.setApiKey("your-api-key");  // 可选
config.setDefaultModel("hermes-agent");
```

### 2. 初始化客户端

```java
import io.github.hiwepy.hermes.HermesClient;

try (HermesClient client = new HermesClient(config)) {
    // 使用客户端
}
```

### 3. 健康检查

```java
HealthStatus health = client.health();
System.out.println("状态: " + health.getStatus());
```

### 4. Chat Completion

```java
import io.github.hiwepy.hermes.model.ChatCompletionRequest;
import io.github.hiwepy.hermes.model.ChatCompletionResponse;

ChatCompletionRequest request = new ChatCompletionRequest();
request.setModel("hermes-agent");
request.addMessage("user", "你好，Hermes！");

ChatCompletionResponse response = client.chatCompletion(request);
System.out.println(response.getChoices().get(0).getMessage().getContent());
```

### 5. 创建会话并聊天

```java
// 创建会话
Session session = client.createSession("我的会话");
String sessionId = session.getId();

// 在会话中聊天
ChatCompletionResponse response = client.sessionChat(sessionId, "讲个笑话");
```

### 6. 创建并监控 Run

```java
import io.github.hiwepy.hermes.model.RunCreateRequest;
import io.github.hiwepy.hermes.model.RunStatus;

RunCreateRequest request = new RunCreateRequest();
request.setSessionId(sessionId);
request.addMessage("user", "分析这些数据...");

RunStatus run = client.createRun(request);
String runId = run.getId();

// 轮询等待完成
RunStatus status;
do {
    Thread.sleep(1000);
    status = client.getRun(runId);
} while ("in_progress".equals(status.getStatus()));
```

### 7. SSE 流式

```java
import io.github.hiwepy.hermes.http.HermesSseClient;

HermesSseClient sse = client.sse();
sse.subscribe("/events", event -> {
    System.out.println("事件: " + event.getData());
});
```

### 8. 本地 CLI

```java
import io.github.hiwepy.hermes.cli.HermesCli;

HermesCli cli = client.cli();
String version = cli.version();
```

## 配置参考

### `HermesClientConfig`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `serverUrl` | `String` | `http://localhost:8642` | Hermes API Server 根地址 |
| `apiKey` | `String` | `null` | Bearer 认证令牌 |
| `connectTimeoutMillis` | `int` | `15000` | 连接超时（毫秒） |
| `readTimeoutMillis` | `int` | `300000` | 读取超时（毫秒）— Run 请求可能耗时较长 |
| `verifySsl` | `boolean` | `true` | 验证 HTTPS 证书 |
| `localExecutable` | `String` | `hermes` | 本地 CLI 可执行文件路径 |
| `localTimeoutSeconds` | `int` | `300` | CLI 命令超时（秒） |
| `localProbeTimeoutSeconds` | `int` | `5` | CLI 启动探测超时（秒） |
| `defaultModel` | `String` | `hermes-agent` | 默认模型名称 |
| `defaultInstructions` | `String` | `null` | 默认系统指令 |
| `defaultProvider` | `String` | `null` | 默认 Provider 名称 |

## 启动检查配置

SDK 支持自动启动可用性检查：

### HTTP 启动检查

```java
HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
httpConfig.setEnabled(true);                    // 启用 HTTP 客户端
httpConfig.setStartupCheckEnabled(true);        // 启动时检查
httpConfig.setFailFastOnUnavailable(false);     // 不可用时不快速失败
```

### CLI 启动检查

```java
HermesCliConfig cliConfig = new HermesCliConfig();
cliConfig.setEnabled(true);                     // 启用 CLI
cliConfig.setStartupCheckEnabled(true);         // 启动时检查
cliConfig.setFailFastOnUnavailable(false);      // 不可用时不快速失败
```

### 统一配置

```java
HermesClientConfig config = new HermesClientConfig();
// HTTP 配置
config.setHttp(httpConfig);
// CLI 配置
config.setCli(cliConfig);
```

## API 参考

### HermesClient

| 方法 | 说明 |
|------|------|
| `health()` | 健康检查 |
| `chatCompletion(request)` | 同步聊天补全 |
| `createRun(request)` | 创建新的 Agent Run |
| `getRun(runId)` | 获取 Run 状态 |
| `stopRun(runId)` | 停止正在运行的 Run |
| `createSession(title)` | 创建新会话 |
| `listSessions()` | 列出所有会话 |
| `getSession(sessionId)` | 获取会话详情 |
| `deleteSession(sessionId)` | 删除会话 |
| `sessionChat(sessionId, input)` | 在会话中聊天 |
| `listModels()` | 列出可用模型 |
| `getCapabilities()` | 获取系统能力 |
| `sse()` | 获取 SSE 客户端 |
| `cli()` | 获取 CLI 客户端 |

### 数据模型

| 类 | 说明 |
|----|------|
| `ChatCompletionRequest` | 聊天补全请求 |
| `ChatCompletionResponse` | 聊天补全响应 |
| `RunCreateRequest` | Run 创建请求 |
| `RunStatus` | Run 状态 |
| `Session` | 会话对象 |
| `ModelInfo` | 模型信息 |
| `CapabilityInfo` | 系统能力 |
| `HealthStatus` | 健康状态 |
| `Message` | 聊天消息 |
| `SseEvent` | SSE 事件 |

## 项目结构

```
io.github.hiwepy.hermes
├── HermesClient              # 统一客户端门面
├── HermesClientConfig        # 配置 POJO
├── http
│   ├── HermesHttpClient      # HTTP REST 客户端
│   └── HermesSseClient       # SSE 流式客户端
├── cli
│   ├── HermesCli             # CLI 封装
│   ├── HermesCliExecutor     # CLI 命令执行器
│   └── HermesCliResult       # CLI 结果封装
├── model                     # 数据模型
│   ├── ChatCompletionRequest/Response
│   ├── RunCreateRequest/RunStatus
│   ├── Session
│   ├── Message
│   └── ...
├── exception                 # 异常层次
└── util                      # 工具类
```

## 完整示例

```java
import io.github.hiwepy.hermes.HermesClient;
import io.github.hiwepy.hermes.HermesClientConfig;
import io.github.hiwepy.hermes.model.*;

public class HermesExample {
    public static void main(String[] args) {
        // 配置
        HermesClientConfig config = new HermesClientConfig();
        config.setServerUrl("http://localhost:8642");
        config.setApiKey(System.getenv("HERMES_API_KEY"));
        
        // 初始化
        try (HermesClient client = new HermesClient(config)) {
            // 健康检查
            HealthStatus health = client.health();
            System.out.println("服务器状态: " + health.getStatus());
            
            // 聊天
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel("hermes-agent");
            request.addMessage("user", "什么是 Java？");
            
            ChatCompletionResponse response = client.chatCompletion(request);
            System.out.println(response.getChoices().get(0).getMessage().getContent());
            
            // 会话
            Session session = client.createSession("演示会话");
            System.out.println("会话已创建: " + session.getId());
        }
    }
}
```

## 构建

```bash
mvn clean install
```

## 测试

```bash
mvn test
```

## 许可证

Apache License 2.0

## 相关项目

- [hermes-spring-boot-starter](../hermes-spring-boot-starter) — Spring Boot 自动配置
- [Hermes Agent](https://github.com/easy-4-java/hermes) — Hermes Agent 服务端
