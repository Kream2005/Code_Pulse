@echo off
REM Starts codepulse-search on Windows (no Docker).
setlocal
cd /d "%~dp0"

if not exist ".venv\Scripts\python.exe" (
  echo ==^> Creating .venv and installing requirements
  py -3 -m venv .venv
  if errorlevel 1 (
    echo Python 3 not found. Install from https://www.python.org/downloads/ and re-run.
    exit /b 1
  )
  call .venv\Scripts\python.exe -m pip install -U pip
  call .venv\Scripts\pip.exe install -r requirements.txt
)

if not exist ".env" (
  echo ==^> Copying .env.example to .env
  copy /Y .env.example .env >nul
)

set PORT=8090
for /f "usebackq tokens=1,* delims==" %%A in (`.env`) do (
  if /I "%%A"=="SERVICE_PORT" set PORT=%%B
)

echo ==^> Starting codepulse-search on http://127.0.0.1:%PORT%
call .venv\Scripts\uvicorn.exe app.main:app --host 0.0.0.0 --port %PORT%
