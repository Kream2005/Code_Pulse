@echo off
setlocal
cd /d "%~dp0\.."

echo ==^> CodePulse STANDALONE mode (Postgres + HTTP publisher, no Kafka/Mailpit/Docker)
echo     Requires: PostgreSQL on localhost:5432
echo     App:  http://localhost:4200
echo     Demo seed (on first/standalone start): ~48 challenges, ~28 candidates,
echo             ~16 questions, ~36 feedbacks, ~40 inbox notifs, ~18 password resets
echo     Admin: admin@codepulse.local / Admin1234!
echo     User:  demo.user@codepulse.local / Demo1234!
echo     Challenge admin: challenge.admin@codepulse.local / Challenge1234!
echo     Manager RH: manager.rh@codepulse.local / Manager1234!
echo     Toggle: codepulse.mode=standalone in backend\src\main\resources\application.properties
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

if exist "challenge-publisher\.venv\Scripts\python.exe" (
  start "CodePulse-Publisher" cmd /k "cd /d "%cd%\challenge-publisher" && .venv\Scripts\python.exe publisher.py --mode http --interval 20 --batch-size 5 --email demo.user@codepulse.local --user-id 90002"
) else (
  start "CodePulse-Publisher" cmd /k "cd /d "%cd%\challenge-publisher" && python publisher.py --mode http --interval 20 --batch-size 5 --email demo.user@codepulse.local --user-id 90002"
)

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
echo Close the terminal windows to stop the app.
endlocal
