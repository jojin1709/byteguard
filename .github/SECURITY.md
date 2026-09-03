# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 1.x.x (latest) | ✅ |
| < 1.0.0 | ❌ |

## Reporting a Vulnerability

> [!CAUTION]
> **Do NOT open a public GitHub Issue for security vulnerabilities.** This exposes the problem to everyone before a fix is available.

If you discover a security vulnerability in **Sentinel Java Agent**, please report it responsibly:

### How to Report

1. **Email**: Send details to the repository owner via GitHub private contact
2. **GitHub Security Advisory**: Use the [private vulnerability reporting](https://github.com/jojin1709/byteguard/security/advisories/new) feature on this repository

### What to Include

- A clear description of the vulnerability
- Steps to reproduce (proof of concept if possible)
- Affected version(s)
- Potential impact and severity
- Suggested fix (optional)

### Response Timeline

| Stage | Target Time |
|---|---|
| Acknowledgement | Within 48 hours |
| Initial assessment | Within 7 days |
| Fix or mitigation | Within 30 days (critical: 7 days) |

### Scope

This project is a **Java bytecode instrumentation framework**. The following are in scope:

- Unsafe bytecode generation that could cause JVM crashes
- Transformer logic that could introduce privilege escalation
- Dependency vulnerabilities (ASM, JUnit)
- Incorrect handling of class loading isolation

### Out of Scope

- Issues in the target application being instrumented (not this project's responsibility)
- Social engineering attacks
- Denial of service via deliberate misuse of agent arguments

---

*Sentinel Java Agent — by JOJIN JOHN*
