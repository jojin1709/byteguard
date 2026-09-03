#!/usr/bin/env bash
# ============================================================
#  Sentinel Java Agent — Linux/macOS Run Helper
#  Author: JOJIN JOHN
# ============================================================
#  Usage:
#    ./run-with-agent.sh             (default REPLACE mode)
#    ./run-with-agent.sh TRACE       (TRACE mode)
#    ./run-with-agent.sh COUNT       (COUNT mode)
# ============================================================

AGENT_JAR="target/sentinel-agent.jar"
MODE="${1:-REPLACE}"

echo "[Sentinel] Starting with mode=$MODE"
java -javaagent:"$AGENT_JAR"=mode="$MODE",verbose=true -jar "$AGENT_JAR"
