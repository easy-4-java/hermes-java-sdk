# hermes-java-sdk

> Pure Java SDK for Hermes Agent — HTTP API Server + SSE streaming + local CLI integration.

[English](README.md) | [中文](README.zh_CN.md)

## Overview

`hermes-java-sdk` is a pure Java SDK (no Spring dependency) for integrating with [Hermes Agent](https://github.com/easy-4-java/hermes). It provides three independent communication channels:

| Channel | Description | Entry Point |
|---------|-------------|-------------|
| **HTTP** | REST API for Runs, Chat Completions, Sessions, Models | `HermesHttpClient` |
| **SSE** | Server-Sent Events streaming | `HermesSseClient` |
| **CLI** | Local `hermes` command execution | `HermesCli` |

For Spring Boot applications, use [hermes-spring-boot-starter](../hermes-spring-boot-starter).

## Features

- **Unified Client Facade** — `HermesClient` provides a single entry point for all three channels
- **Chat Completions** — Synchronous chat completion API
- **Runs** — Create, query, stop agent runs
- **Sessions** — Create, list, get, delete sessions; chat within sessions
- **Models & Capabilities** — List available models and system capabilities
- **SSE Streaming** — Subscribe to real-time Server-Sent Events
- **Local CLI** — Execute local `hermes` commands with structured output parsing
- **Health Checks** — Server health endpoint integration
- **Startup Probes** — Configurable startup availability checks for HTTP and CLI

## Requirements

- Java 8+ (JDK 8 for 1.0.x, JDK 17 for 2.0.x+)
- Hermes Agent server running (default: `http://localhost:8642`)

## Installation

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

## Quick Start

### 1. Create Configuration

```java
import io.github.hiwepy.hermes.HermesClientConfig;

HermesClientConfig config = new HermesClientConfig();
config.setServerUrl("http://localhost:8642");
config.setApiKey("your-api-key");  // Optional
config.setDefaultModel("hermes-agent");
```

### 2. Initialize Client

```java
import io.github.hiwepy.hermes.HermesClient;

try (HermesClient client = new HermesClient(config)) {
    // Use client
}
```

### 3. Health Check

```java
HealthStatus health = client.health();
System.out.println("Status: " + health.getStatus());
```

### 4. Chat Completion

```java
import io.github.hiwepy.hermes.model.ChatCompletionRequest;
import io.github.hiwepy.hermes.model.ChatCompletionResponse;

ChatCompletionRequest request = new ChatCompletionRequest();
request.setModel("hermes-agent");
request.addMessage("user", "Hello, Hermes!");

ChatCompletionResponse response = client.chatCompletion(request);
System.out.println(response.getChoices().get(0).getMessage().getContent());
```

### 5. Create Session and Chat

```java
// Create session
Session session = client.createSession("My Session");
String sessionId = session.getId();

// Chat within session
ChatCompletionResponse response = client.sessionChat(sessionId, "Tell me a joke");
```

### 6. Create and Monitor Run

```java
import io.github.hiwepy.hermes.model.RunCreateRequest;
import io.github.hiwepy.hermes.model.RunStatus;

RunCreateRequest request = new RunCreateRequest();
request.setSessionId(sessionId);
request.addMessage("user", "Analyze this data...");

RunStatus run = client.createRun(request);
String runId = run.getId();

// Poll for completion
RunStatus status;
do {
    Thread.sleep(1000);
    status = client.getRun(runId);
} while ("in_progress".equals(status.getStatus()));
```

### 7. SSE Streaming

```java
import io.github.hiwepy.hermes.http.HermesSseClient;

HermesSseClient sse = client.sse();
sse.subscribe("/events", event -> {
    System.out.println("Event: " + event.getData());
});
```

### 8. Local CLI

```java
import io.github.hiwepy.hermes.cli.HermesCli;

HermesCli cli = client.cli();
String version = cli.version();
```

## Configuration Reference

### `HermesClientConfig`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `serverUrl` | `String` | `http://localhost:8642` | Hermes API Server root URL |
| `apiKey` | `String` | `null` | Bearer token for authentication |
| `connectTimeoutMillis` | `int` | `15000` | Connection timeout (ms) |
| `readTimeoutMillis` | `int` | `300000` | Read timeout (ms) — runs may take longer |
| `verifySsl` | `boolean` | `true` | Verify HTTPS certificates |
| `localExecutable` | `String` | `hermes` | Local CLI executable path |
| `localTimeoutSeconds` | `int` | `300` | CLI command timeout (seconds) |
| `localProbeTimeoutSeconds` | `int` | `5` | CLI startup probe timeout (seconds) |
| `defaultModel` | `String` | `hermes-agent` | Default model name |
| `defaultInstructions` | `String` | `null` | Default system prompt |
| `defaultProvider` | `String` | `null` | Default provider name |

## Startup Check Configuration

The SDK supports automatic startup availability checks:

### HTTP Startup Check

```java
HermesHttpClientConfig httpConfig = new HermesHttpClientConfig();
httpConfig.setEnabled(true);                    // Enable HTTP client
httpConfig.setStartupCheckEnabled(true);        // Check at startup
httpConfig.setFailFastOnUnavailable(false);     // Don't fail if unavailable
```

### CLI Startup Check

```java
HermesCliConfig cliConfig = new HermesCliConfig();
cliConfig.setEnabled(true);                     // Enable CLI
cliConfig.setStartupCheckEnabled(true);         // Check at startup
cliConfig.setFailFastOnUnavailable(false);      // Don't fail if unavailable
```

### Unified Configuration

```java
HermesClientConfig config = new HermesClientConfig();
// HTTP settings
config.setHttp(httpConfig);
// CLI settings
config.setCli(cliConfig);
```

## API Reference

### HermesClient

| Method | Description |
|--------|-------------|
| `health()` | Health check |
| `chatCompletion(request)` | Synchronous chat completion |
| `createRun(request)` | Create a new agent run |
| `getRun(runId)` | Get run status |
| `stopRun(runId)` | Stop a running run |
| `createSession(title)` | Create a new session |
| `listSessions()` | List all sessions |
| `getSession(sessionId)` | Get session details |
| `deleteSession(sessionId)` | Delete a session |
| `sessionChat(sessionId, input)` | Chat within a session |
| `listModels()` | List available models |
| `getCapabilities()` | Get system capabilities |
| `sse()` | Get SSE client |
| `cli()` | Get CLI client |

### Models

| Class | Description |
|-------|-------------|
| `ChatCompletionRequest` | Chat completion request |
| `ChatCompletionResponse` | Chat completion response |
| `RunCreateRequest` | Run creation request |
| `RunStatus` | Run status |
| `Session` | Session object |
| `ModelInfo` | Model information |
| `CapabilityInfo` | System capabilities |
| `HealthStatus` | Health status |
| `Message` | Chat message |
| `SseEvent` | SSE event |

## Project Structure

```
io.github.hiwepy.hermes
├── HermesClient              # Unified client facade
├── HermesClientConfig        # Configuration POJO
├── http
│   ├── HermesHttpClient      # HTTP REST client
│   └── HermesSseClient       # SSE streaming client
├── cli
│   ├── HermesCli             # CLI wrapper
│   ├── HermesCliExecutor     # CLI command executor
│   └── HermesCliResult       # CLI result wrapper
├── model                     # Data models
│   ├── ChatCompletionRequest/Response
│   ├── RunCreateRequest/RunStatus
│   ├── Session
│   ├── Message
│   └── ...
├── exception                 # Exception hierarchy
└── util                      # Utilities
```

## Examples

### Full Example

```java
import io.github.hiwepy.hermes.HermesClient;
import io.github.hiwepy.hermes.HermesClientConfig;
import io.github.hiwepy.hermes.model.*;

public class HermesExample {
    public static void main(String[] args) {
        // Configure
        HermesClientConfig config = new HermesClientConfig();
        config.setServerUrl("http://localhost:8642");
        config.setApiKey(System.getenv("HERMES_API_KEY"));
        
        // Initialize
        try (HermesClient client = new HermesClient(config)) {
            // Health check
            HealthStatus health = client.health();
            System.out.println("Server status: " + health.getStatus());
            
            // Chat
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel("hermes-agent");
            request.addMessage("user", "What is Java?");
            
            ChatCompletionResponse response = client.chatCompletion(request);
            System.out.println(response.getChoices().get(0).getMessage().getContent());
            
            // Session
            Session session = client.createSession("Demo Session");
            System.out.println("Session created: " + session.getId());
        }
    }
}
```

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## License

Apache License 2.0

## Related Projects

- [hermes-spring-boot-starter](../hermes-spring-boot-starter) — Spring Boot auto-configuration
- [Hermes Agent](https://github.com/easy-4-java/hermes) — Hermes Agent server
