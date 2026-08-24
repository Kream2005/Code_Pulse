@echo off
setlocal EnableExtensions
if not defined KAFKA_HOME (
  echo KAFKA_HOME is not set.
  exit /b 1
)
set "KAFKA_BIN=%KAFKA_HOME%\bin\windows"
if not exist "%KAFKA_BIN%\kafka-server-stop.bat" set "KAFKA_BIN=%KAFKA_HOME%\bin"
if exist "%KAFKA_BIN%\kafka-server-stop.bat" (
  call "%KAFKA_BIN%\kafka-server-stop.bat"
)
echo Kafka stop requested
endlocal
