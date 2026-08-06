# hermes-java-sdk

[English](./README.md) | [简体中文](./README.zh-CN.md)

![Java](https://img.shields.io/badge/Java-8-orange) ![License](https://img.shields.io/badge/License-Apache%202.0-blue)

A pure Java SDK for [Hermes Agent](https://github.com/easy-4-java/hermes) — HTTP API Server + SSE streaming + local CLI integration. Spring-free, JDK 8 baseline.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

`hermes-java-sdk` is a pure Java SDK (no Spring dependency) for integrating with Hermes Agent, an agent service with an HTTP API server. The SDK provides three independent communication channels behind a single facade:

| Channel | Description | Entry point |
|:---|:---|:---|
| **HTTP** | REST API for health, chat completions, responses, runs, sessions, models, capabilities, skills, toolsets and jobs | `HermesHttpClient` |
| **SSE** | Server-Sent Events streaming (chat, run events, session streams) | `HermesSseClient` |
| **CLI** | Local `hermes` command execution with structured output parsing | `HermesCli` |

| What it is | What it is not |
|:---|:---|
| A unified client for a Hermes Agent server | A Spring Boot starter (no auto-configuration) |
| Typed request/response models for the agent API | An agent implementation itself |
| Local CLI wrapper + startup probes | A UI |

Typical use cases:

| Use case | Notes |
|:---|:---|
| Health checks before traffic | `health()`, `healthDetailed()`, `healthV1()` |
| Chat completions | `chatCompletion(ChatRequest)`, `chatCompletionWithSession(...)` |
| Streaming chat | `chatCompletionStream(ChatRequest)` -> `ChatStreamingResponse` |
| Agent runs | `createRun(RunCreateRequest)`, `getRun`, `stopRun`, `approveRun` |
| Sessions | `createSession(title)`, `listSessions()`, `sessionChat(sessionId, input)`, `forkSession`, `deleteSession` |
| Responses API | `createResponse(ResponseRequest)`, `getResponse`, `deleteResponse` |
| Local CLI | `cli().chatOneShot(query)`, `cli().worktreeOneShot(query)`, `cli().version()` |
| Jobs | `listJobs()`, `createJob`, `getJob`, `updateJob`, `pauseJob`, `resumeJob`, `runJobNow` |

**Project status:** active development.

## 2. Features & Status

| Feature | Status | Notes |
|:---|:---|:---|
| Unified `HermesClient` facade | Available | Single entry point for HTTP + SSE + CLI channels (`AutoCloseable`) |
| Chat completions (sync) | Available | `chatCompletion(ChatRequest[, headers])`, with-session variants |
| SSE streaming | Available | `chatCompletionStream(ChatRequest)`, `sse().subscribeChat(...)`, `subscribeQueue(runId)` |
| Runs | Available | `createRun` / `getRun` / `stopRun` / `approveRun` |
| Sessions | Available | Create / list / get / messages / fork / delete / `sessionChat` |
| Responses API | Available | `createResponse` / `getResponse` / `deleteResponse` |
| Models & capabilities | Available | `listModels()`, `getModel(modelId)`, `getCapabilities()`, `listSkills()`, `listToolsets()` |
| Jobs | Available | List / create / get / update / delete / pause / resume / run-now |
| Local CLI | Available | `HermesCli` + `HermesCliExecutor.execute(...)`, `probe()` |
| Startup probes | Available | HTTP and CLI availability checkers; `failFastOnUnavailable` flags |
| Health endpoints | Available | `/health`, `/health/detailed`, `/v1/health` |
| Integration test | Available | `HermesClientIntegrationTest` (test sources) |
| CI pipeline | Not configured | No CI workflow files in the repository |

## 3. Requirements & Compatibility

| Requirement | Version |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| Hermes Agent server | Running instance (default `http://localhost:8642`) |
| OkHttp | 4.12.0 |
| Jackson | 2.22.0 (`jackson-databind`) |

### Version lines

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

### Server API endpoints (as declared in `HermesApiConstants`)

| Path | Purpose |
|:---|:---|
| `/health`, `/health/detailed`, `/v1/health` | Health |
| `/v1/chat/completions` | Chat completions |
| `/v1/responses` | Responses API |
| `/v1/models`, `/v1/capabilities`, `/v1/skills`, `/v1/toolsets` | Models / capabilities / skills / toolsets |
| `/v1/runs` | Agent runs |
| `/api/sessions` | Sessions |
| `/api/jobs` | Jobs |

## 4. Architecture & Modules

```text
  Your code                    hermes-java-sdk                    Hermes Agent
  ---------                    --------------                    ------------
  Request models  ->  HermesClient (facade)
                          |         |          \
                          |         |           \
                    HermesHttpClient   HermesSseClient     HermesCli
                    (OkHttp 3 +         (SSE events,          (local `hermes`
                     Jackson)            queue/consumer)       subprocess)
                          |                |                    |
                          +----- HTTP -----+                    +---- subprocess
                                |                                       |
                    /v1/* REST endpoints                      local CLI
                    (serverUrl default                       (executable
                     http://localhost:8642)                   `hermes`)
```

Single module, jar packaging:

| Package | Responsibility |
|:---|:---|
| `io.github.easy4j.hermes` | `HermesClient`, configs (`HermesClientConfig`, `HermesHttpClientConfig`, `HermesCliConfig`) |
| `io.github.easy4j.hermes.api` | `HermesHttpClient`, `HermesSseClient`, `HermesApiConstants` |
| `io.github.easy4j.hermes.api.model` | Typed models (`ChatRequest`, `ChatResponse`, `Session`, `RunStatus`, `HealthStatus`, `SseEvent`, ...) |
| `io.github.easy4j.hermes.cli` | `HermesCli`, `HermesCliExecutor`, `HermesCliResult` |
| `io.github.easy4j.hermes.cli.availability` | CLI startup probe |
| `io.github.easy4j.hermes.exception` | Typed exceptions (`HermesException`, `HermesHttpException`, `HermesCliStartupException`) |
| `io.github.easy4j.hermes.util` | `HermesObjectMapper`, `HermesJsonParser` |

## 5. Installation

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>hermes-java-sdk</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:hermes-java-sdk:1.0.x.20260630-SNAPSHOT'
```

**Availability:** the artifact is published to the Aliyun private Maven repository and distributed through GitHub Releases; it has not yet been published to Maven Central.

## 6. Quick Start

```java
import io.github.easy4j.hermes.HermesClient;
import io.github.easy4j.hermes.HermesClientConfig;
import io.github.easy4j.hermes.api.model.ChatRequest;
import io.github.easy4j.hermes.api.model.ChatResponse;
import io.github.easy4j.hermes.api.model.HealthStatus;
import io.github.easy4j.hermes.api.model.Message;

HermesClientConfig config = new HermesClientConfig();
config.getHttp().setServerUrl("http://localhost:8642");   // Hermes Agent server
config.getHttp().setApiKey("optional-api-key");           // optional

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

Expected result: the health status is printed, and the assistant reply content is printed from the typed `ChatResponse`.

## 7. Configuration

Configuration is grouped in `HermesClientConfig`, which holds an `HermesHttpClientConfig` (`getHttp()`) and an `HermesCliConfig` (`getCli()`).

`HermesHttpClientConfig`:

| Property | Default | Description |
|:---|:---|:---|
| `enabled` | `true` | Enable the HTTP channel |
| `startupCheckEnabled` | `false` | Run a startup probe for the HTTP channel |
| `failFastOnUnavailable` | `false` | Fail fast when the probe reports unavailable |
| `serverUrl` | `http://localhost:8642` | Hermes Agent server base URL |
| `apiKey` | — | Optional API key header |
| `connectTimeoutMillis` | `15000` | Connect timeout |
| `readTimeoutMillis` | `300000` | Read timeout |
| `verifySsl` | `true` | TLS verification |
| `defaultModel` | `hermes-agent` | Default model name |
| `defaultInstructions` / `defaultProvider` | — | Optional defaults |

`HermesCliConfig`:

| Property | Default | Description |
|:---|:---|:---|
| `enabled` | `true` | Enable the CLI channel |
| `startupCheckEnabled` | `false` | Run a startup probe for the CLI |
| `failFastOnUnavailable` | `false` | Fail fast when the CLI is unavailable |
| `executable` | `hermes` | Local `hermes` executable name or path |
| `timeout` | `300` | Command timeout (seconds) |
| `probeTimeoutSeconds` | `5` | Probe timeout (seconds) |
| `workingDirectory` | — | Working directory |
| `maxConcurrentExecutions` | `0` | Max concurrent child processes (`<=0`: max(CPU cores, 2)) |

## 8. Core Usage / API

### 8.1 Chat with a session

```java
Session session = client.createSession("My Session");
ChatResponse reply = client.sessionChat(session.getId(), "Tell me a joke");
```

### 8.2 Create and monitor a run

```java
RunCreateRequest runRequest = new RunCreateRequest();
runRequest.setSessionId(session.getId());
runRequest.setInput("Analyze this data...");

RunStatus run = client.createRun(runRequest);
String runId = run.getId();
RunStatus current = client.getRun(runId);
client.stopRun(runId);
```

### 8.3 SSE streaming

```java
ChatStreamingResponse stream = client.chatCompletionStream(request);
// or raw access:
client.sse().subscribeChat(request, sseEvent -> {
    System.out.println(sseEvent.getType() + ": " + sseEvent.getData());
});
```

### 8.4 Local CLI

```java
HermesCliResult oneShot = client.cli().chatOneShot("hello");  // hermes -z hello
HermesCliResult version = client.cli().version();             // hermes --version
```

### 8.5 HTTP vs CLI channel selection

`HermesClient.isHttpEnabled()` / `isCliEnabled()` report which channels are active. When both are enabled, high-level methods such as `chatCompletion` route through the HTTP channel; CLI calls are explicit through `cli()`.

## 9. Testing & Build

```bash
./mvnw clean verify        # compile, run tests, generate coverage report
./mvnw clean install       # install into the local repository
```

- An integration test (`HermesClientIntegrationTest`) exists in the test sources.
- Coverage is measured with the JaCoCo Maven plugin (target: 90% line coverage, `haltOnFailure=false`).
- The `release` profile assembles GPG signing + sources + Javadoc + deployment (`./mvnw -Prelease clean deploy`).

## 10. Versioning & Branches

Three parallel version lines are maintained:

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

Maintenance strategy: the 1.0.x line receives bug fixes while JDK 8 remains the baseline; feature development primarily targets the 2.0.x / 3.0.x lines.

## 11. Contributing & License

Contributions are welcome — open an issue or submit a pull request against the matching version-line branch (`feature/1.0.x` for JDK 8 changes).

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0). See the `LICENSE` file in the repository root for details.
