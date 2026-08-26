#!/usr/bin/env bash
# Starts codepulse-search as a local process (no Docker).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [[ ! -x .venv/bin/python ]]; then
  echo "==> Creating .venv and installing requirements (Phase 0 core)"
  python3 -m venv .venv
  .venv/bin/pip install -U pip
  .venv/bin/pip install -r requirements.txt
fi

if [[ ! -f .env ]]; then
  cp .env.example .env
fi

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

PORT="${SERVICE_PORT:-8090}"
exec .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port "$PORT"
