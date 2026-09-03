# Sentinel Java Agent — Complete Usage Guide

> **Developer:** JOJIN JOHN | **Version:** 1.0.0 | **Java:** 17+

---

## Quick Start

```bash
# 1. Build
mvn clean package

# 2. Run WITHOUT agent (original behaviour)
java -jar target/sentinel-agent.jar

# 3. Run WITH agent (default REPLACE mode)
java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar
```

---

## Configuration

The agent accepts `key=value` pairs as agent arguments:

```bash
java -javaagent:sentinel-agent.jar=key1=value1,key2=value2 -jar app.jar
```

### All Configuration Keys

| Key | Description | Default |
|---|---|---|
| `target` | Semicolon-separated class patterns (glob supported) | `demo/target/TargetService` |
| `method` | Method name to instrument | `message` |
| `mode` | `REPLACE` \| `TRACE` \| `COUNT` \| `NULL_CHECK` | `REPLACE` |
| `verbose` | Log every class scan | `false` |
| `logfile` | Path to a log file (appended) | none |
| `config` | Path to a `.properties` config file | none |

---

## Target Patterns (Wildcards)

The `target` key supports glob-style wildcards:

| Pattern | Matches |
|---|---|
| `demo/target/TargetService` | Exact class only |
| `demo/target/*` | All classes in `demo.target` package |
| `com/example/**` | All classes in `com.example` and sub-packages |
| `**/Service` | Any class named `Service` in any package |
| `demo/target/Target*` | Classes starting with `Target` in `demo.target` |

### Multiple Targets

Separate patterns with semicolons:

```bash
java -javaagent:sentinel-agent.jar=target=demo/target/*;com/example/Service,mode=TRACE -jar app.jar
```

---

## Config File

Create `sentinel.properties`:

```properties
sentinel.target=demo/target/TargetService
sentinel.method=message
sentinel.mode=TRACE
sentinel.verbose=true
sentinel.logfile=sentinel.log
```

Load it with:

```bash
java -javaagent:sentinel-agent.jar=config=/path/to/sentinel.properties -jar app.jar
```

> Inline args override file values. File is loaded first.

---

## Transformation Modes

### REPLACE
Replaces the return value of the target method with `"Transformed message"`.

```bash
java -javaagent:sentinel-agent.jar=mode=REPLACE -jar app.jar
```

Output:
```
[Sentinel] ENTER message()
Transformed message
```

---

### TRACE
Logs method entry, exit, return value, and elapsed time in milliseconds.

```bash
java -javaagent:sentinel-agent.jar=mode=TRACE -jar app.jar
```

Output:
```
[Sentinel] ENTER message()
[Sentinel] EXIT  message() — returned: Original message — took 2ms
```

---

### COUNT
Counts each method invocation atomically and prints the running total.

```bash
java -javaagent:sentinel-agent.jar=mode=COUNT -jar app.jar
```

Output:
```
[Sentinel] message() call #1
[Sentinel] message() call #2
[Sentinel] message() call #3
```

---

### NULL_CHECK
Checks whether a reference-returning method returns `null`. Logs a warning to stderr if it does.

```bash
java -javaagent:sentinel-agent.jar=mode=NULL_CHECK -jar app.jar
```

Output (if method returns non-null):
```
[Sentinel] NULL_CHECK active on message()
```

Output (if method returns null):
```
[Sentinel] NULL_CHECK WARNING: message() returned null!
```

---

## Log File

Enable file logging alongside console output:

```bash
java -javaagent:sentinel-agent.jar=mode=TRACE,logfile=agent.log -jar app.jar
```

The log file uses timestamped format:
```
2026-09-03 22:00:00.123 [INFO] [Sentinel] Transformed: demo.target.TargetService [mode=TRACE]
2026-09-03 22:00:00.124 [INFO] [Sentinel] ENTER message()
```

---

## Dynamic Attach

Attach to an **already running** JVM without restart:

```bash
# List all running JVMs
java --add-modules jdk.attach -jar sentinel-agent.jar --list

# Attach to PID 12345
java --add-modules jdk.attach -jar sentinel-agent.jar --attach 12345

# Attach with configuration
java --add-modules jdk.attach -jar sentinel-agent.jar --attach 12345 mode=TRACE,verbose=true
```

> **Requires JDK** (not JRE) on the machine running the attach command.

---

## Statistics

On JVM shutdown, a summary is automatically printed:

```
[Sentinel] ─── Statistics ──────────────────────────────
[Sentinel]   Classes scanned    : 1,247
[Sentinel]   Classes transformed:     1
[Sentinel]   Transform errors   :     0
[Sentinel]   Method invocations :     3
[Sentinel] ─────────────────────────────────────────────
```

---

## Helper Scripts

```bash
# Windows
scripts\run-with-agent.bat TRACE

# Linux / macOS
./scripts/run-with-agent.sh TRACE
```
