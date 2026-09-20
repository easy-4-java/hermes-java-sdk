# 原方案到 OpenSpec 的追溯矩阵

原文：[优化方案 V1.0](../design/hermes-java-sdk-optimization-plan-v1.0.md)。计划编号保留，未实施不得勾选。完整机器索引为 [traceability.json](traceability.json)。
## 原 28 项实施任务

| 原编号 | 归属任务（change:task） |
|---|---|
| H-001 | `harden-hermes-transport:1.1` |
| H-002 | `harden-hermes-transport:1.2` |
| H-003 | `harden-hermes-transport:1.3` |
| H-004 | `harden-hermes-transport:1.4` |
| H-101 | `harden-hermes-transport:2.1`；`harden-hermes-transport:2.2`；`harden-hermes-transport:2.3` |
| H-102 | `harden-hermes-transport:3.1`；`harden-hermes-transport:3.2`；`harden-hermes-transport:3.3` |
| H-103 | `harden-hermes-transport:4.1`；`harden-hermes-transport:4.2`；`harden-hermes-transport:4.3` |
| H-104 | `harden-hermes-transport:5.1`；`harden-hermes-transport:5.2`；`harden-hermes-transport:5.3` |
| H-105 | `harden-hermes-transport:6.1`；`harden-hermes-transport:6.2`；`harden-hermes-transport:6.3` |
| H-106 | `harden-hermes-transport:7.1`；`harden-hermes-transport:7.2`；`harden-hermes-transport:7.3`；`harden-hermes-transport:7.4` |
| H-201 | `complete-hermes-http-runtime:1.1`；`complete-hermes-http-runtime:1.2`；`complete-hermes-http-runtime:1.3` |
| H-202 | `complete-hermes-http-runtime:2.1`；`complete-hermes-http-runtime:2.2`；`complete-hermes-http-runtime:2.3` |
| H-203 | `complete-hermes-http-runtime:3.1`；`complete-hermes-http-runtime:3.2`；`complete-hermes-http-runtime:3.3`；`complete-hermes-http-runtime:3.4` |
| H-204 | `complete-hermes-http-runtime:4.1`；`complete-hermes-http-runtime:4.2`；`complete-hermes-http-runtime:4.3` |
| H-205 | `complete-hermes-http-runtime:5.1`；`complete-hermes-http-runtime:5.2`；`complete-hermes-http-runtime:5.3`；`complete-hermes-http-runtime:5.4` |
| H-206 | `complete-hermes-http-runtime:6.1`；`complete-hermes-http-runtime:6.2`；`complete-hermes-http-runtime:6.3`；`complete-hermes-http-runtime:6.4` |
| H-301 | `enhance-hermes-cli-runtime:1.1`；`enhance-hermes-cli-runtime:1.2`；`enhance-hermes-cli-runtime:1.3`；`enhance-hermes-cli-runtime:1.4` |
| H-302 | `enhance-hermes-cli-runtime:2.1`；`enhance-hermes-cli-runtime:2.2`；`enhance-hermes-cli-runtime:2.3`；`enhance-hermes-cli-runtime:2.4` |
| H-303 | `enhance-hermes-cli-runtime:3.1`；`enhance-hermes-cli-runtime:3.2`；`enhance-hermes-cli-runtime:3.3`；`enhance-hermes-cli-runtime:3.4` |
| H-304 | `enhance-hermes-cli-runtime:4.1`；`enhance-hermes-cli-runtime:4.2`；`enhance-hermes-cli-runtime:4.3` |
| H-401 | `add-hermes-acp-client:1.1`；`add-hermes-acp-client:1.2`；`add-hermes-acp-client:1.3`；`add-hermes-acp-client:1.4` |
| H-402 | `add-hermes-acp-client:2.1`；`add-hermes-acp-client:2.2`；`add-hermes-acp-client:2.3`；`add-hermes-acp-client:2.4` |
| H-403 | `add-hermes-acp-client:3.1`；`add-hermes-acp-client:3.2`；`add-hermes-acp-client:3.3`；`add-hermes-acp-client:3.4` |
| H-404 | `add-hermes-acp-client:4.1`；`add-hermes-acp-client:4.2`；`add-hermes-acp-client:4.3`；`add-hermes-acp-client:4.4` |
| H-501 | `harden-hermes-transport:8.1`；`harden-hermes-transport:8.2`；`complete-hermes-http-runtime:7.1`；`enhance-hermes-cli-runtime:5.1`；`add-hermes-acp-client:5.1` |
| H-502 | `harden-hermes-transport:8.3`；`complete-hermes-http-runtime:7.2`；`enhance-hermes-cli-runtime:5.2`；`add-hermes-acp-client:5.2` |
| H-503 | `harden-hermes-transport:8.4`；`complete-hermes-http-runtime:7.3`；`enhance-hermes-cli-runtime:5.3`；`add-hermes-acp-client:5.3` |
| H-504 | `harden-hermes-transport:8.5`；`complete-hermes-http-runtime:7.4`；`enhance-hermes-cli-runtime:5.4`；`add-hermes-acp-client:5.4` |

## 原 28 个验收场景

| 原编号 | 新 Scenario |
|---|---|
| T-01 | EP-001-S1 |
| T-02 | EP-001-S2, EP-003-S1 |
| T-03 | PA-001-S1, PA-003-S2 |
| T-04 | PA-002-S1, PA-002-S2 |
| T-05 | SE-001-S1 |
| T-06 | RS-002-S1 |
| T-07 | SE-003-S1 |
| T-08 | SE-004-S1 |
| T-09 | SE-005-S1 |
| T-10 | SE-006-S1 |
| T-11 | SE-002-S1 |
| T-12 | RN-003-S1 |
| T-13 | RN-003-S2 |
| T-14 | RN-004-S1, RN-004-S2 |
| T-15 | AP-001-S1, AP-002-S2 |
| T-16 | RC-001-S1 |
| T-17 | RC-002-S1 |
| T-18 | RC-001-S2 |
| T-19 | BC-001-S1 |
| T-20 | CS-001-S1, CE-003-S1 |
| T-21 | CS-002-S1 |
| T-22 | CS-002-S2 |
| T-23 | CE-004-S1 |
| T-24 | AT-002-S1, AM-002-S1 |
| T-25 | AT-003-S1, AT-003-S2 |
| T-26 | SE-002-S2, RN-002-S2, RR-003-S1 |
| T-27 | RC-003-S1 |
| T-28 | OB-003-S1 |

## Requirement 到实施任务与测试

| Requirement | 能力域 | 目标测试类 | 实施任务 |
|---|---|---|---|
| EP-001 | trusted-endpoints | `EndpointPolicyContractTest.java` | harden-hermes-transport:2.1, harden-hermes-transport:2.2, harden-hermes-transport:2.3 |
| EP-002 | trusted-endpoints | `EndpointPolicyContractTest.java` | harden-hermes-transport:2.1, harden-hermes-transport:2.2, harden-hermes-transport:2.3 |
| EP-003 | trusted-endpoints | `EndpointPolicyContractTest.java` | harden-hermes-transport:2.1, harden-hermes-transport:2.2, harden-hermes-transport:2.3 |
| PA-001 | profile-authentication | `ProfileAuthenticationContractTest.java` | harden-hermes-transport:3.1, harden-hermes-transport:3.2, harden-hermes-transport:3.3 |
| PA-002 | profile-authentication | `ProfileAuthenticationContractTest.java` | harden-hermes-transport:3.1, harden-hermes-transport:3.2, harden-hermes-transport:3.3 |
| PA-003 | profile-authentication | `ProfileAuthenticationContractTest.java` | harden-hermes-transport:3.1, harden-hermes-transport:3.2, harden-hermes-transport:3.3 |
| SE-001 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:4.1, harden-hermes-transport:4.2, harden-hermes-transport:4.3 |
| SE-002 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:4.1, harden-hermes-transport:4.2, harden-hermes-transport:4.3 |
| SE-003 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:5.1, harden-hermes-transport:5.2, harden-hermes-transport:5.3 |
| SE-004 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:5.1, harden-hermes-transport:5.2, harden-hermes-transport:5.3 |
| SE-005 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:5.1, harden-hermes-transport:5.2, harden-hermes-transport:5.3 |
| SE-006 | sse-lifecycle | `SseLifecycleContractTest.java` | harden-hermes-transport:6.1, harden-hermes-transport:6.2, harden-hermes-transport:6.3 |
| RC-001 | request-cancellation | `PublicCancellationContractTest.java` | harden-hermes-transport:7.1, harden-hermes-transport:7.2, harden-hermes-transport:7.3, harden-hermes-transport:7.4 |
| RC-002 | request-cancellation | `PublicCancellationContractTest.java` | harden-hermes-transport:7.1, harden-hermes-transport:7.2, harden-hermes-transport:7.3, harden-hermes-transport:7.4 |
| RC-003 | request-cancellation | `PublicCancellationContractTest.java` | harden-hermes-transport:7.1, harden-hermes-transport:7.2, harden-hermes-transport:7.3, harden-hermes-transport:7.4 |
| BC-001 | sdk-compatibility | `SdkCompatibilityContractTest.java` | harden-hermes-transport:8.1, harden-hermes-transport:8.2, harden-hermes-transport:8.3, harden-hermes-transport:8.4, harden-hermes-transport:8.5 |
| BC-002 | sdk-compatibility | `SdkCompatibilityContractTest.java` | harden-hermes-transport:1.1, harden-hermes-transport:1.2, harden-hermes-transport:1.3, harden-hermes-transport:1.4, harden-hermes-transport:8.1, harden-hermes-transport:8.2, harden-hermes-transport:8.3, harden-hermes-transport:8.4, harden-hermes-transport:8.5 |
| BC-003 | sdk-compatibility | `SdkCompatibilityContractTest.java` | harden-hermes-transport:8.1, harden-hermes-transport:8.2, harden-hermes-transport:8.3, harden-hermes-transport:8.4, harden-hermes-transport:8.5 |
| CT-001 | request-context | `RequestContextContractTest.java` | complete-hermes-http-runtime:1.1, complete-hermes-http-runtime:1.2, complete-hermes-http-runtime:1.3 |
| CT-002 | request-context | `RequestContextContractTest.java` | complete-hermes-http-runtime:1.1, complete-hermes-http-runtime:1.2, complete-hermes-http-runtime:1.3 |
| CT-003 | request-context | `RequestContextContractTest.java` | complete-hermes-http-runtime:1.1, complete-hermes-http-runtime:1.2, complete-hermes-http-runtime:1.3 |
| RS-001 | responses-streaming | `ResponsesStreamingContractTest.java` | complete-hermes-http-runtime:2.1, complete-hermes-http-runtime:2.2, complete-hermes-http-runtime:2.3 |
| RS-002 | responses-streaming | `ResponsesStreamingContractTest.java` | complete-hermes-http-runtime:2.1, complete-hermes-http-runtime:2.2, complete-hermes-http-runtime:2.3 |
| RS-003 | responses-streaming | `ResponsesStreamingContractTest.java` | complete-hermes-http-runtime:2.1, complete-hermes-http-runtime:2.2, complete-hermes-http-runtime:2.3 |
| RN-001 | run-lifecycle | `RunLifecycleContractTest.java` | complete-hermes-http-runtime:3.1, complete-hermes-http-runtime:3.2, complete-hermes-http-runtime:3.3, complete-hermes-http-runtime:3.4 |
| RN-002 | run-lifecycle | `RunLifecycleContractTest.java` | complete-hermes-http-runtime:3.1, complete-hermes-http-runtime:3.2, complete-hermes-http-runtime:3.3, complete-hermes-http-runtime:3.4 |
| RN-003 | run-lifecycle | `RunLifecycleContractTest.java` | complete-hermes-http-runtime:3.1, complete-hermes-http-runtime:3.2, complete-hermes-http-runtime:3.3, complete-hermes-http-runtime:3.4 |
| RN-004 | run-lifecycle | `RunLifecycleContractTest.java` | complete-hermes-http-runtime:3.1, complete-hermes-http-runtime:3.2, complete-hermes-http-runtime:3.3, complete-hermes-http-runtime:3.4 |
| AP-001 | approval-workflow | `ApprovalWorkflowContractTest.java` | complete-hermes-http-runtime:4.1, complete-hermes-http-runtime:4.2, complete-hermes-http-runtime:4.3 |
| AP-002 | approval-workflow | `ApprovalWorkflowContractTest.java` | complete-hermes-http-runtime:4.1, complete-hermes-http-runtime:4.2, complete-hermes-http-runtime:4.3 |
| RR-001 | runtime-resources | `RuntimeResourcesContractTest.java` | complete-hermes-http-runtime:5.1, complete-hermes-http-runtime:5.2, complete-hermes-http-runtime:5.3, complete-hermes-http-runtime:5.4 |
| RR-002 | runtime-resources | `RuntimeResourcesContractTest.java` | complete-hermes-http-runtime:5.1, complete-hermes-http-runtime:5.2, complete-hermes-http-runtime:5.3, complete-hermes-http-runtime:5.4 |
| RR-003 | runtime-resources | `RuntimeResourcesContractTest.java` | complete-hermes-http-runtime:5.1, complete-hermes-http-runtime:5.2, complete-hermes-http-runtime:5.3, complete-hermes-http-runtime:5.4 |
| OB-001 | runtime-observability | `RuntimeObservabilityContractTest.java` | complete-hermes-http-runtime:6.1, complete-hermes-http-runtime:6.2, complete-hermes-http-runtime:6.3, complete-hermes-http-runtime:6.4 |
| OB-002 | runtime-observability | `RuntimeObservabilityContractTest.java` | complete-hermes-http-runtime:6.1, complete-hermes-http-runtime:6.2, complete-hermes-http-runtime:6.3, complete-hermes-http-runtime:6.4 |
| OB-003 | runtime-observability | `RuntimeObservabilityContractTest.java` | complete-hermes-http-runtime:6.1, complete-hermes-http-runtime:6.2, complete-hermes-http-runtime:6.3, complete-hermes-http-runtime:6.4 |
| CE-001 | cli-execution | `CliExecutionContractTest.java` | enhance-hermes-cli-runtime:1.1, enhance-hermes-cli-runtime:1.2, enhance-hermes-cli-runtime:1.3, enhance-hermes-cli-runtime:1.4 |
| CE-002 | cli-execution | `CliExecutionContractTest.java` | enhance-hermes-cli-runtime:1.1, enhance-hermes-cli-runtime:1.2, enhance-hermes-cli-runtime:1.3, enhance-hermes-cli-runtime:1.4 |
| CE-003 | cli-execution | `CliExecutionContractTest.java` | enhance-hermes-cli-runtime:1.1, enhance-hermes-cli-runtime:1.2, enhance-hermes-cli-runtime:1.3, enhance-hermes-cli-runtime:1.4 |
| CE-004 | cli-execution | `CliExecutionContractTest.java` | enhance-hermes-cli-runtime:3.1, enhance-hermes-cli-runtime:3.2, enhance-hermes-cli-runtime:3.3, enhance-hermes-cli-runtime:3.4 |
| CS-001 | cli-streaming | `CliStreamingContractTest.java` | enhance-hermes-cli-runtime:2.1, enhance-hermes-cli-runtime:2.2, enhance-hermes-cli-runtime:2.3, enhance-hermes-cli-runtime:2.4 |
| CS-002 | cli-streaming | `CliStreamingContractTest.java` | enhance-hermes-cli-runtime:2.1, enhance-hermes-cli-runtime:2.2, enhance-hermes-cli-runtime:2.3, enhance-hermes-cli-runtime:2.4 |
| CS-003 | cli-streaming | `CliStreamingContractTest.java` | enhance-hermes-cli-runtime:2.1, enhance-hermes-cli-runtime:2.2, enhance-hermes-cli-runtime:2.3, enhance-hermes-cli-runtime:2.4 |
| CC-001 | cli-commands | `CliCommandsContractTest.java` | enhance-hermes-cli-runtime:4.1, enhance-hermes-cli-runtime:4.2, enhance-hermes-cli-runtime:4.3 |
| CC-002 | cli-commands | `CliCommandsContractTest.java` | enhance-hermes-cli-runtime:4.1, enhance-hermes-cli-runtime:4.2, enhance-hermes-cli-runtime:4.3 |
| AT-001 | acp-transport | `AcpTransportContractTest.java` | add-hermes-acp-client:1.1, add-hermes-acp-client:1.2, add-hermes-acp-client:1.3, add-hermes-acp-client:1.4, add-hermes-acp-client:4.1, add-hermes-acp-client:4.2, add-hermes-acp-client:4.3, add-hermes-acp-client:4.4 |
| AT-002 | acp-transport | `AcpTransportContractTest.java` | add-hermes-acp-client:1.1, add-hermes-acp-client:1.2, add-hermes-acp-client:1.3, add-hermes-acp-client:1.4 |
| AT-003 | acp-transport | `AcpTransportContractTest.java` | add-hermes-acp-client:3.1, add-hermes-acp-client:3.2, add-hermes-acp-client:3.3, add-hermes-acp-client:3.4, add-hermes-acp-client:4.1, add-hermes-acp-client:4.2, add-hermes-acp-client:4.3, add-hermes-acp-client:4.4 |
| AS-001 | acp-sessions | `AcpSessionsContractTest.java` | add-hermes-acp-client:2.1, add-hermes-acp-client:2.2, add-hermes-acp-client:2.3, add-hermes-acp-client:2.4, add-hermes-acp-client:4.1, add-hermes-acp-client:4.2, add-hermes-acp-client:4.3, add-hermes-acp-client:4.4 |
| AS-002 | acp-sessions | `AcpSessionsContractTest.java` | add-hermes-acp-client:2.1, add-hermes-acp-client:2.2, add-hermes-acp-client:2.3, add-hermes-acp-client:2.4 |
| AS-003 | acp-sessions | `AcpSessionsContractTest.java` | add-hermes-acp-client:2.1, add-hermes-acp-client:2.2, add-hermes-acp-client:2.3, add-hermes-acp-client:2.4 |
| AM-001 | acp-permissions | `AcpPermissionsContractTest.java` | add-hermes-acp-client:3.1, add-hermes-acp-client:3.2, add-hermes-acp-client:3.3, add-hermes-acp-client:3.4 |
| AM-002 | acp-permissions | `AcpPermissionsContractTest.java` | add-hermes-acp-client:3.1, add-hermes-acp-client:3.2, add-hermes-acp-client:3.3, add-hermes-acp-client:3.4, add-hermes-acp-client:4.1, add-hermes-acp-client:4.2, add-hermes-acp-client:4.3, add-hermes-acp-client:4.4 |