@echo off
REM ============================================================
REM  Sentinel Java Agent — Windows Run Helper
REM  Author: JOJIN JOHN
REM ============================================================
REM  Usage:
REM    run-with-agent.bat             (default REPLACE mode)
REM    run-with-agent.bat TRACE       (TRACE mode)
REM    run-with-agent.bat COUNT       (COUNT mode)
REM ============================================================

set AGENT_JAR=target\sentinel-agent.jar
set MODE=%1
if "%MODE%"=="" set MODE=REPLACE

echo [Sentinel] Starting with mode=%MODE%
java -javaagent:%AGENT_JAR%=mode=%MODE%,verbose=true -jar %AGENT_JAR%
