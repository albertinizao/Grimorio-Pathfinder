@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "MVN_CMD=%SCRIPT_DIR%.tools\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MVN_CMD%" (
  call "%MVN_CMD%" %*
) else (
  mvn %*
)
