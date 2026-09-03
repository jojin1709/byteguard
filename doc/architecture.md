# Architecture Overview

## How the Agent Works

```
JVM startup
    │
    ├─► premain(agentArgs, instrumentation)
    │       │
    │       ├─► AgentConfig.parse(agentArgs)   ← reads args or .properties file
    │       ├─► AgentLogger.init(logFile)       ← opens log file if configured
    │       └─► instrumentation.addTransformer(new Loader(config), canRetransform=true)
    │
    └─► Class loading loop
            │
            ├─► Loader.transform(className, bytecode)
            │       │
            │       ├─► AgentStats.recordScan()
            │       ├─► AgentConfig.matches(className)   ← PatternMatcher glob check
            │       │       └─► return null if not matched (pass-through)
            │       │
            │       └─► switch(mode)
            │               ├─► REPLACE    → applyReplace()   ← ASM Tree API
            │               ├─► TRACE      → applyTrace()     ← ASM ClassVisitor + AdviceAdapter
            │               ├─► COUNT      → applyCount()     ← ASM ClassVisitor + AdviceAdapter
            │               └─► NULL_CHECK → applyNullCheck() ← ASM ClassVisitor + AdviceAdapter
            │
            └─► ClassWriter.COMPUTE_FRAMES recomputes stack-map frames
                    └─► JVM loads modified bytecode
```

## Key Classes

| Class | Role |
|---|---|
| `sentinel.Loader` | `ClassFileTransformer` — the agent core |
| `sentinel.AgentConfig` | Parses `-javaagent` args and `.properties` files |
| `sentinel.AgentLogger` | Dual stdout + file logger |
| `sentinel.AgentStats` | Thread-safe scan/transform/error/invocation counters |
| `sentinel.PatternMatcher` | Glob wildcard pattern matching for class names |
| `sentinel.TransformMode` | Enum: REPLACE, TRACE, COUNT, NULL_CHECK |
| `sentinel.AttachTool` | CLI for attaching to running JVMs |
| `sentinel.Filter` | String normalization utilities |

## Technology Stack

- **Java 17** — modern JVM features (records, sealed classes, pattern matching)
- **OW2 ASM 9.7.1** — bytecode manipulation
  - `asm` — core ClassReader/ClassWriter
  - `asm-tree` — Tree API for REPLACE mode (ClassNode, MethodNode)
  - `asm-commons` — AdviceAdapter for TRACE/COUNT/NULL_CHECK modes
- **JUnit 5** — unit tests
- **Maven Shade Plugin** — self-contained fat JAR with bundled ASM

## Bytecode Transformation Internals

### REPLACE (Tree API)
Reads all instructions into a `ClassNode`, finds the target `MethodNode`, clears its `InsnList`, and inserts:
```
LDC "Transformed message"
ARETURN
```

### TRACE (Visitor API + AdviceAdapter)
`onMethodEnter` injects:
```
INVOKESTATIC System.nanoTime()J
LSTORE startTime
GETSTATIC System.out
LDC "[Sentinel] ENTER method()"
INVOKEVIRTUAL println
```
`onMethodExit(ARETURN)` injects:
```
DUP                             ← preserve return value on stack
ASTORE retVar                   ← capture for logging
INVOKESTATIC System.nanoTime()J
LLOAD startTime
LSUB
LDC 1_000_000L
LDIV
LSTORE msVar
// StringBuilder log with retVar + msVar
```

### COUNT (Visitor API + AdviceAdapter)
`onMethodEnter` injects:
```
INVOKESTATIC AgentStats.recordInvocation()J
LSTORE count
// StringBuilder log with count
```

### NULL_CHECK (Visitor API + AdviceAdapter)
`onMethodExit(ARETURN)` injects:
```
DUP
IFNONNULL label_ok
GETSTATIC System.err
LDC "[Sentinel] NULL_CHECK WARNING: method() returned null!"
INVOKEVIRTUAL println
label_ok:
// original return value still on stack
```
