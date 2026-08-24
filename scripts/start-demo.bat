@echo off
setlocal EnableExtensions
cd /d "%~dp0\.."
set "RESOURCES=%cd%\backend\src\main\resources"

echo ==^> CodePulse STANDALONE mode (Postgres + Kafka binary + embedded SMTP, no Docker)
echo     Requires: PostgreSQL on localhost:5432  (scripts\create-db.sql once)
echo     Requires: Kafka binary — set KAFKA_HOME to the unpacked folder
echo     Email: GreenMail on :1025 — GET /dev/mailbox as admin@codepulse.local
echo     App:  http://localhost:4200
echo     API:  http://localhost:8080
echo     Accounts:
echo       USER:                   demo.user@codepulse.local / Demo1234!
echo       ADMIN_CODING_CHALLENGE: challenge.admin@codepulse.local / Challenge1234!
echo       MANAGER_RH:             manager.rh@codepulse.local / Manager1234!
echo       ADMIN_CODEPULSE:        admin@codepulse.local / Admin1234!
echo.

where java >nul 2>&1
if errorlevel 1 (
  echo Java 21 is required. Install JDK 21 and add it to PATH.
  exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
  echo Node.js 25+ is required. Install Node and add it to PATH.
  exit /b 1
)

if not exist "%RESOURCES%\application.properties" (
  echo Creating application.properties from example...
  copy /Y "%RESOURCES%\application.properties.example" "%RESOURCES%\application.properties" >nul
  echo Edit spring.datasource.* if your Postgres password differs.
  echo Default DB ^(scripts\create-db.sql^): codepulse / codepulse / codepulse
)

if not exist "%RESOURCES%\private.key" (
  where openssl >nul 2>&1
  if errorlevel 1 (
    echo ERROR: JWT keys missing and openssl not found.
    echo Install OpenSSL or place private.key + public.key in backend\src\main\resources\
    exit /b 1
  )
  echo Generating demo JWT RSA keys...
  openssl genrsa -out "%RESOURCES%\private.key" 2048
  openssl rsa -in "%RESOURCES%\private.key" -pubout -out "%RESOURCES%\public.key"
)

echo Starting local Kafka...
call "%~dp0kafka-start.bat"
if errorlevel 1 (
  echo Kafka did not start. Set KAFKA_HOME and JAVA_HOME, then retry.
  exit /b 1
)

if not exist "challenge-publisher\.venv\Scripts\python.exe" (
  echo Creating publisher venv...
  python -m venv "challenge-publisher\.venv"
  "challenge-publisher\.venv\Scripts\pip.exe" install -r "challenge-publisher\requirements.txt"
)

start "CodePulse-Publisher" cmd /k "cd /d "%cd%\challenge-publisher" && .venv\Scripts\python.exe publisher.py --mode both --interval 20 --batch-size 5 --email demo.user@codepulse.local --user-id 90002"

start "CodePulse-API" cmd /k "cd /d "%cd%\backend" && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--codepulse.mode=standalone"
timeout /t 8 /nobreak >nul

cd frontend
if not exist node_modules (
  echo Installing frontend dependencies...
  call npm install
)
start "CodePulse-UI" cmd /k "cd /d "%cd%" && npm start"

echo.
echo Open http://localhost:4200 when Vite is ready.
echo Captured emails: GET http://localhost:8080/dev/mailbox  (login as admin)
echo Close the terminal windows to stop the app.
endlocal
