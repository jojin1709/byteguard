# Sentinel Java Agent

> [!NOTE]
> **[Sentinel 1.0.0 is live](https://github.com/sentinel/burp-agent):** Production-grade Java 17 instrumentation engine, dynamic bytecode rewriting powered by OW2 ASM 9.7.1, automatic stack-map frame computation, idempotent retransformation hooks, and zero-dependency runtime isolation.

<div align="center">

<h1>🛡️ Sentinel Java Agent</h1>

<p><strong>A high-performance Java 17 runtime instrumentation agent for dynamic bytecode analysis, method interception, and non-destructive in-memory transformation.</strong></p>

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![ASM](https://img.shields.io/badge/OW2_ASM-9.7.1-blue?style=for-the-badge)](https://asm.ow2.io/)
[![JUnit](https://img.shields.io/badge/JUnit-5.11-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Developer](https://img.shields.io/badge/Developer-JOJIN%20JOHN-8A2BE2?style=for-the-badge)]()
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)](./LICENSE)
[![CI](https://github.com/jojin1709/byteguard/actions/workflows/maven.yml/badge.svg)](https://github.com/jojin1709/byteguard/actions/workflows/maven.yml)
[![Release](https://img.shields.io/github/v/release/jojin1709/byteguard?style=for-the-badge&color=brightgreen&label=Release)](https://github.com/jojin1709/byteguard/releases/latest)

<br/>

```bash
# Build the self-contained agent and run the demo
mvn clean package
java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar
```

<sub>Built for authorized security research, application telemetry, dynamic mocking, and bytecode analysis.</sub>

---

</div>

> [!TIP]
> **Quick Start:** Jump directly to [Quick Start](#quick-start) to compile and execute the demo target with and without the agent attached.

---

## Table of Contents

- [Table of Contents](#table-of-contents)
- [What is Sentinel?](#what-is-sentinel)
  - [Why Sentinel Exists](#why-sentinel-exists)
  - [Why "Sentinel"?](#why-sentinel)
  - [Authorized Instrumentation & Research Only](#authorized-instrumentation--research-only)
- [Sentinel in Action](#sentinel-in-action)
  - [Terminal Output Comparison](#terminal-output-comparison)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Build Sentinel](#build-sentinel)
  - [Run Demo Without Agent](#run-demo-without-agent)
  - [Run Demo With Agent](#run-demo-with-agent)
- [Architecture](#architecture)
  - [Instrumentation Pipeline](#instrumentation-pipeline)
  - [How the Bytecode Transformation Works](#how-the-bytecode-transformation-works)
- [Project Structure & Files](#project-structure--files)
- [Key Capabilities](#key-capabilities)
- [Troubleshooting & Gotchas](#troubleshooting--gotchas)
- [Safety, Scope, and Ethics](#safety-scope-and-ethics)
- [Common Questions](#common-questions)
- [Developer & Author](#developer--author)

---

## What is Sentinel?

**Sentinel** is a Java Instrumentation Agent (`-javaagent`) engineered to inspect, intercept, and rewrite JVM bytecode in-flight as classes are loaded into memory. Built with **Java 17** and the industry-standard **OW2 ASM 9.7.1 Tree API**, it provides a rock-solid, production-grade foundation for runtime security research, APM telemetry, and dynamic patching.

Unlike naive agents that pollute the classpath, crash on custom ClassLoaders, or break JVM verification, Sentinel implements strict null-safety, class-scope isolation, automatic stack-map frame recalculation (`COMPUTE_FRAMES`), and retransformation idempotency.

<a id="why-sentinel-exists"></a>
<details>
<summary><strong>Why Sentinel Exists</strong></summary>

Static source-code modifications require recompiling, repackaging, and redeploying entire application suites. In dynamic analysis, security testing, and APM observability, engineers must be able to observe and modify runtime behavior non-destructively.

Sentinel establishes a clean, robust harness for modifying compiled classes without altering on-disk binaries, ensuring full compatibility with Java 17's strict module and verifier constraints.

</details>

<a id="why-sentinel"></a>
<details>
<summary><strong>Why "Sentinel"?</strong></summary>

A sentinel stands watch at the gates. In the JVM, Sentinel stands at the classloader boundary, evaluating byte streams before they are linked, verified, and executed.

</details>

<a id="authorized-instrumentation--research-only"></a>
<details>
<summary><strong>Authorized Instrumentation & Research Only</strong></summary>

Sentinel is strictly scoped to authorized applications under the tester's control. It enforces strict package boundaries to prevent tampering with system classes, third-party software, or commercial license validation mechanisms.

</details>

---

## Sentinel in Action

Sentinel demonstrates surgical bytecode rewriting on a controlled target service: `demo.target.TargetService`.

| Method | Original Bytecode Behavior | With Sentinel Agent Attached | State |
| :--- | :--- | :--- | :--- |
| `message()` | Returns `"Original message"` | Returns `"Transformed message"` | **Modified via ASM Tree API** |
| `add(int, int)` | Computes arithmetic sum `a + b` | Computes arithmetic sum `a + b` | **Preserved untouched** |

### Terminal Output Comparison

```text
======================= WITHOUT AGENT =======================
$ java -jar target/sentinel-agent.jar
Original message
5

======================== WITH AGENT =========================
$ java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar
[Sentinel] Agent initialized (retransformation support: true)
[Sentinel] Inspecting demo.target.TargetService
[Sentinel] Transforming TargetService.message()
[Sentinel] Transformation complete
Transformed message
5
```

---

## Quick Start

### Prerequisites

- **Java JDK 17+** (Eclipse Adoptium Temurin, Oracle JDK, or OpenJDK)
- **Apache Maven 3.9+**

Verify your environment:
```bash
java -version
mvn -version
```

### Build Sentinel

Compile sources, run automated unit tests, and assemble the self-contained agent JAR:

```bash
mvn clean package
```

The output artifact is generated at:
```text
target/sentinel-agent.jar
```

### Run Demo Without Agent

Execute the demo application directly from the packaged JAR or classpath:

```bash
# Executing packaged executable JAR
java -jar target/sentinel-agent.jar

# Or via explicit classpath
java -cp target/sentinel-agent.jar demo.app.DemoApplication
```

**Expected output:**
```text
Original message
5
```

### Run Demo With Agent

Attach Sentinel via `-javaagent` at JVM startup:

```bash
# Executing packaged executable JAR with agent attached
java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar

# Or via explicit classpath
java -javaagent:target/sentinel-agent.jar -cp target/sentinel-agent.jar demo.app.DemoApplication
```

**Expected output:**
```text
[Sentinel] Agent initialized (retransformation support: true)
[Sentinel] Inspecting demo.target.TargetService
[Sentinel] Transforming TargetService.message()
[Sentinel] Transformation complete
Transformed message
5
```

---

## Architecture

### Instrumentation Pipeline

Sentinel hooks into the JVM's `java.lang.instrument` subsystem before the application `main()` method is invoked:

```mermaid
flowchart TD
    JVM["JVM Launch<br/><code>java -javaagent:...</code>"] --> PRE["sentinel.Loader.premain()"]
    PRE --> REG["Register ClassFileTransformer<br/><code>canRetransform = true</code>"]
    REG --> APP_START["Application Starts Main Class"]

    APP_START --> CL["JVM ClassLoader loads<br/><code>demo/target/TargetService</code>"]
    CL --> HOOK["sentinel.Loader.transform()"]

    HOOK --> GATE{"Class == demo/target/TargetService?"}
    GATE -- "No" --> PASS["Return null<br/>(Pass through unmodified)"]
    GATE -- "Yes" --> PARSE["ASM ClassReader<br/><code>SKIP_FRAMES</code>"]

    PARSE --> SCAN["Scan Methods via ASM Tree API"]
    SCAN --> IDEM{"TargetService.message()<br/>already transformed?"}
    IDEM -- "Yes" --> SKIP["Return null<br/>(Idempotency Guard)"]
    IDEM -- "No" --> INJECT["Replace Instructions:<br/><code>LDC 'Transformed message'</code><br/><code>ARETURN</code>"]

    INJECT --> WRITER["ASM ClassWriter<br/><code>COMPUTE_FRAMES</code>"]
    WRITER --> VERIFY["JVM Bytecode Linker & Verifier"]
    VERIFY --> EXEC["TargetService Executes with<br/>New Message!"]
```

### How the Bytecode Transformation Works

1. **ASM Tree Representation**: The raw class byte buffer is parsed by `ClassReader` into an in-memory `ClassNode`.
2. **Method Lookup**: Sentinel iterates over `classNode.methods` looking for `message` with descriptor `()Ljava/lang/String;`.
3. **Idempotency Verification**: Sentinel checks if the method body already loads `"Transformed message"`. If found (such as during JVM class retransformation), transformation is skipped.
4. **Instruction Replacement**:
   - Clears existing instruction list (`method.instructions.clear()`).
   - Injects symbolic ASM instructions:
     ```java
     InsnList newInstructions = new InsnList();
     newInstructions.add(new LdcInsnNode("Transformed message"));
     newInstructions.add(new InsnNode(Opcodes.ARETURN));
     method.instructions.add(newInstructions);
     ```
5. **Frame Recomputation**: Uses `ClassWriter(ClassWriter.COMPUTE_FRAMES)` which automatically calculates stack map frames and maximum operand stack depth (`COMPUTE_MAXS`), ensuring the class satisfies Java 17 verification rules.

---

## Project Structure & Files

```text
burp-agent/
├── pom.xml                                   # Build configuration, plugins, dependencies
├── README.md                                 # Project documentation and operational guide
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── demo/
    │   │   │   ├── app/
    │   │   │   │   └── DemoApplication.java  # Demo entry point executing TargetService
    │   │   │   └── target/
    │   │   │       └── TargetService.java    # Demo target class containing message() and add()
    │   │   └── sentinel/
    │   │       ├── Filter.java               # Safe string normalization and utility methods
    │   │       └── Loader.java               # Premain agent entrypoint & ClassFileTransformer
    │   └── resources/
    │       └── META-INF/
    │           └── MANIFEST.MF               # Agent manifest (Premain-Class, Retransform flags)
    └── test/
        └── java/
            ├── demo/
            │   └── target/
            │       └── TargetServiceTest.java # TargetService uninstrumented unit tests
            └── sentinel/
                ├── FilterTest.java           # Filter utility unit tests
                └── LoaderTest.java           # Agent transformation, null-safety, and idempotency tests
```

| File | Purpose |
| :--- | :--- |
| **`Loader.java`** | Core agent class implementing `ClassFileTransformer` and `premain()`. Contains the ASM bytecode transformation logic. |
| **`Filter.java`** | Safe utility class for string trimming and JVM class name validation. |
| **`TargetService.java`** | Test service containing `message()` (target for transformation) and `add()` (control method). |
| **`DemoApplication.java`** | Console application that invokes `TargetService` and prints the output. |
| **`MANIFEST.MF`** | Agent manifest declaring `Premain-Class: sentinel.Loader` and `Can-Retransform-Classes: true`. |
| **`pom.xml`** | Maven project configuration packaging a self-contained shaded agent JAR with ASM 9.7.1 and JUnit 5. |

---

## Key Capabilities

- **Robust Null-Safety**: Guards against `null` class names (common for hidden/anonymous classes) and empty byte buffers.
- **Strict Scope Isolation**: Never intercepts or modifies JVM system classes (`java/*`, `javax/*`, `jdk/*`) or arbitrary third-party code.
- **Retransformation Idempotency**: Safe to invoke repeatedly under `Instrumentation.retransformClasses()` without duplicating instructions.
- **Automatic Frame Generation**: Guarantees valid `StackMapTable` attributes using `ClassWriter.COMPUTE_FRAMES`.
- **Thread-Safe Architecture**: The transformer maintains no mutable state and safely handles concurrent classloading across multiple JVM threads.
- **Self-Contained Shaded JAR**: Bundles OW2 ASM 9.7.1 dependencies directly inside `target/sentinel-agent.jar` to prevent `NoClassDefFoundError` on target JVMs.

---

## Troubleshooting & Gotchas

| Error / Symptom | Root Cause | Solution Implemented in Sentinel |
| :--- | :--- | :--- |
| `java.lang.VerifyError` | Mismatched operand stack depth or corrupted `StackMapTable` frames. | Sentinel uses `ClassWriter.COMPUTE_FRAMES` and `ClassReader.SKIP_FRAMES` to regenerate correct frame mappings from scratch. |
| `java.lang.ClassFormatError` | Malformed bytecode, invalid constant pool indices, or illegal opcode sequences. | Sentinel uses high-level ASM `ClassNode` and symbolic constants (`Opcodes.LDC`, `Opcodes.ARETURN`). |
| `NoClassDefFoundError: org/objectweb/asm/...` | Target application ClassLoader does not have ASM on its classpath. | Sentinel is packaged with `maven-shade-plugin`, embedding ASM classes directly into `sentinel-agent.jar`. |
| `ClassCircularityError` | The agent attempts to instrument JDK core classes or classes needed to run the agent itself. | Sentinel strictly checks `if (!TARGET_CLASS.equals(className)) return null;` before loading any ASM classes. |
| `UnsupportedOperationException: class redefinition failed` | Manifest declares `Can-Retransform-Classes: false` or agent registered without `canRetransform=true`. | Aligned manifest (`Can-Retransform-Classes: true`) with registration `instrumentation.addTransformer(new Loader(), true)`. |
| Silent Transformation Failure | Exception thrown inside `transform()` swallowed without logging. | Sentinel catches `Throwable`, logs detailed errors to `System.err`, and returns `null` so the JVM falls back to the original class. |

---

## Safety, Scope, and Ethics

> [!WARNING]
> **Authorized Use Only**: Java Instrumentation allows arbitrary modification of JVM memory structures. Use this technology only on software you own or have explicit written permission to analyze.

- **No Commercial Bypass**: Sentinel contains no mechanisms to disable security checks, bypass licensing logic, forge signatures, or crack commercial software.
- **Auditable & Transparent**: Every transformation logs clear notices to standard output, providing full visibility into modified components.

---

## Common Questions

### Is `COMPUTE_MAXS` required when using `COMPUTE_FRAMES`?
**No.** In OW2 ASM, specifying `ClassWriter.COMPUTE_FRAMES` automatically implies `COMPUTE_MAXS`. The frame computation algorithm inherently calculates the maximum stack depth and local variables required for each frame.

### Why does Sentinel use `ClassReader.SKIP_FRAMES`?
When `ClassWriter.COMPUTE_FRAMES` is used, existing stack map frames are discarded and regenerated from scratch. Passing `ClassReader.SKIP_FRAMES` prevents ASM from parsing unnecessary existing frames, saving CPU cycles and avoiding frame merge conflicts.

### Can Sentinel be attached dynamically at runtime?
Yes. By adding an `agentmain(String agentArgs, Instrumentation inst)` method to `Loader.java` and declaring `Agent-Class: sentinel.Loader` in `MANIFEST.MF`, Sentinel can also be attached to already-running JVM processes via the Java Attach API.

---

## Developer & Author

**Sentinel Java Agent** is developed and architected by **JOJIN JOHN**.

---

<div align="center">
<b>Sentinel Java Agent</b> — Engineered with precision for Java 17 runtime analysis by <b>JOJIN JOHN</b>.
</div>
