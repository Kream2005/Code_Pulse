@echo off
setlocal EnableExtensions
REM Start a local Kafka binary (KRaft, no ZooKeeper, no Docker).
REM Set KAFKA_HOME to the unpacked Kafka folder (contains bin\ and config\).

if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME is not set. Install JDK 17+ ^(21 recommended^) and set JAVA_HOME.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: java.exe not found in JAVA_HOME=%JAVA_HOME%
  exit /b 1
)

if not defined KAFKA_HOME (
  echo ERROR: KAFKA_HOME is not set.
  echo Unpack Apache Kafka and run:  set KAFKA_HOME=C:\path\to\kafka
  exit /b 1
)

set "KAFKA_BIN=%KAFKA_HOME%\bin\windows"
if not exist "%KAFKA_BIN%\kafka-server-start.bat" set "KAFKA_BIN=%KAFKA_HOME%\bin"
if not exist "%KAFKA_BIN%\kafka-server-start.bat" (
  echo ERROR: kafka-server-start.bat not found under %KAFKA_HOME%
  echo KAFKA_HOME must be the unpacked Kafka directory.
  exit /b 1
)

set "CONFIG=%KAFKA_HOME%\config\server.properties"
if not exist "%CONFIG%" set "CONFIG=%KAFKA_HOME%\config\kraft\server.properties"
if not exist "%CONFIG%" (
  echo ERROR: server.properties not found under %KAFKA_HOME%\config
  exit /b 1
)

if exist "%KAFKA_HOME%\config\log4j2.yaml" (
  set "KAFKA_LOG4J_OPTS=-Dlog4j.configurationFile=file:///%KAFKA_HOME:\=/%/config/log4j2.yaml"
)

findstr /B /C:"log.dirs=/tmp" "%CONFIG%" >nul 2>&1
if not errorlevel 1 (
  echo WARN: log.dirs still points to /tmp in server.properties.
  echo Edit %CONFIG% and set for example:
  echo   log.dirs=C:/kafka/kraft-combined-logs
  echo Use forward slashes. Then delete that folder if a previous format failed.
)

netstat -an | findstr /C:":9092" | findstr LISTENING >nul 2>&1
if not errorlevel 1 (
  echo Kafka already listening on :9092
  goto topics
)

set "LOG_DIRS="
for /f "tokens=1,* delims==" %%A in ('findstr /B /C:"log.dirs=" "%CONFIG%"') do set "LOG_DIRS=%%B"
if not defined LOG_DIRS set "LOG_DIRS=%KAFKA_HOME%\kraft-combined-logs"
if not exist "%LOG_DIRS%\meta.properties" (
  echo Formatting Kafka storage at %LOG_DIRS%
  for /f "usebackq delims=" %%I in (`call "%KAFKA_BIN%\kafka-storage.bat" random-uuid`) do set "CLUSTER_ID=%%I"
  call "%KAFKA_BIN%\kafka-storage.bat" format --standalone -t %CLUSTER_ID% -c "%CONFIG%"
  if errorlevel 1 call "%KAFKA_BIN%\kafka-storage.bat" format -t %CLUSTER_ID% -c "%CONFIG%"
)

echo Starting Kafka...
start "CodePulse-Kafka" cmd /k "cd /d "%KAFKA_HOME%" && "%KAFKA_BIN%\kafka-server-start.bat" "%CONFIG%""

echo Waiting for :9092 ...
set /a _n=0
:wait
set /a _n+=1
netstat -an | findstr /C:":9092" | findstr LISTENING >nul 2>&1
if not errorlevel 1 goto topics
if %_n% geq 40 (
  echo Kafka failed to start on :9092
  echo Check the CodePulse-Kafka window and %KAFKA_HOME%\logs
  exit /b 1
)
timeout /t 1 /nobreak >nul
goto wait

:topics
call "%KAFKA_BIN%\kafka-topics.bat" --create --if-not-exists --bootstrap-server localhost:9092 --topic coding-challenges --partitions 3 --replication-factor 1 >nul 2>&1
call "%KAFKA_BIN%\kafka-topics.bat" --create --if-not-exists --bootstrap-server localhost:9092 --topic coding-challenges-dlt --partitions 1 --replication-factor 1 >nul 2>&1
echo Kafka is up  KAFKA_HOME=%KAFKA_HOME%
endlocal
