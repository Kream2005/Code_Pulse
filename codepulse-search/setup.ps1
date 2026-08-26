# Phase 0 setup for Windows work laptop (PowerShell).
# Run from: codepulse-search\
#   powershell -ExecutionPolicy Bypass -File .\setup.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "==> Python venv"
if (-not (Test-Path ".venv\Scripts\python.exe")) {
    py -3 -m venv .venv
}
& .\.venv\Scripts\python.exe -m pip install -U pip
& .\.venv\Scripts\pip.exe install -r requirements.txt

if (-not (Test-Path ".env")) {
    Copy-Item .env.example .env
    Write-Host "==> Created .env from .env.example — edit DB password if needed"
}

Write-Host "==> Env check"
& .\.venv\Scripts\python.exe scripts\check_env.py

Write-Host ""
Write-Host "Next (once, as Postgres admin):"
Write-Host "  psql -U postgres -d codepulse -f scripts\enable_pgvector.sql"
Write-Host "Then:"
Write-Host "  .\.venv\Scripts\python.exe scripts\init_db.py"
Write-Host "  .\run.bat"
Write-Host ""
Write-Host "Optional Ollama (for Phase 3/4): https://ollama.com/download"
Write-Host "  ollama pull llama3.2:1b"
Write-Host ""
Write-Host "Phase 1 ML deps (later, heavier):"
Write-Host "  .\.venv\Scripts\pip.exe install -r requirements-ml.txt"
