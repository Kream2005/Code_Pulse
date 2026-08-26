@echo off
REM Reindex CodePulse content into search_chunk (Phase 1).
setlocal
cd /d "%~dp0\.."
if not exist ".venv\Scripts\python.exe" (
  echo Run setup.ps1 first.
  exit /b 1
)
echo ==^> Installing ML deps if needed...
call .venv\Scripts\pip.exe install -q -r requirements-ml.txt
call .venv\Scripts\python.exe scripts\reindex.py
