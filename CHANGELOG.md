# Changelog

All notable changes to **Sentinel Java Agent** are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/).

---

## [1.0.0] — 2026-09-03

### Added
- `sentinel.Loader` — Java 17 `ClassFileTransformer` premain agent entry point
- `sentinel.Filter` — Safe string normalization and JVM class name validation utilities
- `demo.target.TargetService` — Controlled demo target with `message()` and `add()` methods
- `demo.app.DemoApplication` — Console entry point demonstrating before/after agent behavior
- OW2 ASM 9.7.1 (`asm` + `asm-tree`) Tree API bytecode transformation pipeline
- `ClassWriter.COMPUTE_FRAMES` + `ClassReader.SKIP_FRAMES` for correct Java 17 stack-map frames
- Idempotency guard preventing duplicate instrumentation during retransformation
- Strict class-scope gating — only `demo/target/TargetService` is intercepted
- Thread-safe stateless transformer design
- Structured `[Sentinel]` console logging
- 9 unit tests covering null safety, scope gating, bytecode transformation, add() preservation, and idempotency
- `maven-shade-plugin` producing a self-contained shaded `target/sentinel-agent.jar`
- `MANIFEST.MF` with `Premain-Class`, `Can-Redefine-Classes: true`, `Can-Retransform-Classes: true`, `Built-By: JOJIN JOHN`
- MIT License
- `CONTRIBUTING.md`
- `.gitignore` for Maven/IntelliJ/Eclipse/VS Code
- GitHub Actions CI/CD workflow (`maven.yml`)
- GitHub issue and pull request templates

### Architecture
- Agent registers with `instrumentation.addTransformer(new Loader(), true)` for retransformation support
- ASM Tree API (`ClassNode` → `MethodNode` → `InsnList`) for clean instruction manipulation
- Self-contained fat JAR bundles ASM — no external dependencies required on target JVM
