# Contributing to Sentinel Java Agent

Thank you for your interest in contributing! This document explains how to get started.

> [!NOTE]
> By contributing, you agree that your contributions are for **authorized, ethical, and educational** purposes only. Contributions that add commercial software bypass, license cracking, or third-party piracy tooling will be rejected without review.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [How to Contribute](#how-to-contribute)
- [Code Standards](#code-standards)
- [Running Tests](#running-tests)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

---

## Getting Started

### Prerequisites

- Java JDK 17 or newer
- Apache Maven 3.9 or newer
- Git

### Development Setup

```bash
# Clone the repository
git clone https://github.com/your-username/sentinel-java-agent.git
cd sentinel-java-agent

# Build the project and run all tests
mvn clean package

# Verify agent output without agent
java -jar target/sentinel-agent.jar

# Verify agent output with agent
java -javaagent:target/sentinel-agent.jar -jar target/sentinel-agent.jar
```

---

## Project Structure

```text
burp-agent/
├── src/
│   ├── main/java/
│   │   ├── sentinel/          ← Core agent: Loader.java, Filter.java
│   │   └── demo/              ← Demo classes: TargetService, DemoApplication
│   └── test/java/             ← Unit tests
├── pom.xml                    ← Maven build config
└── README.md
```

---

## How to Contribute

### Good contribution ideas

- **Additional demo transformations** — new example target classes with different transformation patterns
- **Logging improvements** — structured logging (SLF4J, java.util.logging)
- **Multi-target support** — configuring multiple classes via `agentArgs`
- **agentmain support** — dynamic attach API
- **Additional ASM examples** — method entry/exit logging, parameter tracing
- **Documentation** — improve inline JavaDoc, README sections
- **Bug fixes** — any genuine bugs found via testing

---

## Code Standards

- Java 17 syntax and features only
- Use symbolic `Opcodes` constants, **never** raw integer opcode values
- All public methods must have JavaDoc
- No wildcard imports (`import java.util.*`)
- No magic numbers
- No swallowed exceptions — always log or rethrow
- `@author` tag on all new classes

---

## Running Tests

```bash
# Run all unit tests
mvn test

# Full clean build + test + package
mvn clean package
```

All 9 existing tests must continue to pass. New features must include tests.

---

## Submitting a Pull Request

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes with tests
4. Run `mvn clean package` and confirm `BUILD SUCCESS`
5. Commit with a clear message: `git commit -m "feat: add multi-target configuration support"`
6. Push and open a Pull Request against `main`

---

## Reporting Bugs

Open a [GitHub Issue](https://github.com/your-username/sentinel-java-agent/issues) with:

- Java version (`java -version`)
- Maven version (`mvn -version`)
- Full error output
- Steps to reproduce

---

## Feature Requests

Open a [GitHub Discussion](https://github.com/your-username/sentinel-java-agent/discussions) describing:

- The use case
- Why it fits within the project's authorized instrumentation scope
- Any implementation ideas you have
