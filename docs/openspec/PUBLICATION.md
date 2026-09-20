# OpenSpec 发布记录

本文件记录 OpenSpec 文档包从会话沙箱发布到 GitHub 的事实边界。

- 文档包在远程写入之前生成并执行离线检查，因此 `docs/openspec/source-manifest.json`、`verification/openspec/` 和 README 中关于“尚未远程提交”的描述属于**生成时快照**，为保持原验证产物可追溯性不回写修改。
- 2026-09-20 已将同一 59 文件 OpenSpec 文档树发布到三个目标版本线：
  - `feature/1.0.x`: `811028649484a5fbef44c152b3fb5081392491f4`
  - `feature/2.0.x`: `9ccdb0a2ea985a91412b3820ad3009ac9d02fdc1`
  - `feature/3.0.x`: `9e9d3a79fba62849ddacab9833d036d16843a534`
- 本次远程写入只增加 OpenSpec 规范、设计/计划文档、校验脚本和生成时验证证据；没有修改 SDK 业务源码、POM 或既有 README。
- 所有 OpenSpec implementation tasks 仍保持未勾选；文档提交不代表功能实现、真实 Hermes 互操作或官方 OpenSpec CLI 校验已经完成。

后续实现必须按各 change 的 `tasks.md` 执行，并在对应分支记录实际 JDK、Hermes 版本、测试和 CI 证据。
