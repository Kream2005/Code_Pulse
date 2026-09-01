#!/usr/bin/env bash
# Setup for Linux (dev machine).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "==> Python venv"
if [[ ! -x .venv/bin/python ]]; then
  python3 -m venv .venv
fi
.venv/bin/pip install -U pip
.venv/bin/pip install -r requirements.txt

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "==> Created .env from .env.example"
fi

echo "==> Env check"
.venv/bin/python scripts/check_env.py

echo
echo "If pgvector is missing, run once as superuser:"
echo "  sudo -u postgres psql -d codepulse -f scripts/enable_pgvector.sql"
echo "Then:"
echo "  .venv/bin/python scripts/init_db.py"
echo "  ./run.sh"
echo
echo "Optional Ollama: https://ollama.com  →  ollama pull llama3.2:1b"
echo "Phase 1 ML later: .venv/bin/pip install -r requirements-ml.txt"
