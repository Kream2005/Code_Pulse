#!/usr/bin/env bash
# Full mode: Postgres + Kafka. Optional Mailpit for email UI.
# Toggle: codepulse.mode=full in backend application.properties
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_LOG="${TMPDIR:-/tmp}/codepulse-backend-full.log"

# Prefer JDK 21 even if the shell already exported an older JAVA_HOME (e.g. 11).
resolve_java_home() {
  local candidates=(
    "${CODEPULSE_JAVA_HOME:-}"
    "/usr/lib/jvm/jdk-21.0.6-oracle-x64"
    "/usr/lib/jvm/java-21-openjdk-amd64"
    "/usr/lib/jvm/jdk-21"
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ -n "$c" && -x "$c/bin/java" ]]; then
      echo "$c"
      return 0
    fi
  done
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]] \
      && "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
    echo "$JAVA_HOME"
    return 0
  fi
  return 1
}

JAVA_HOME="$(resolve_java_home)" || {
  echo "ERROR: JDK 21 is required but was not found."
  echo "Install JDK 21, or set CODEPULSE_JAVA_HOME=/path/to/jdk-21"
  exit 1
}
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "    Java: $JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -1))"

echo "==> CodePulse FULL mode (Postgres + Kafka + optional Mailpit)"
echo "    Requires: PostgreSQL :5432, Kafka :9092"
echo "    Optional: docker start mailpit  (SMTP :1025, UI :8025)"
echo

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  # shellcheck disable=SC1090
  . "$NVM_DIR/nvm.sh"
  nvm use 25 >/dev/null 2>&1 || true
  export PATH="$NVM_DIR/versions/node/$(nvm version 25 2>/dev/null || echo v25.9.0)/bin:$PATH"
fi

fuser -k 8080/tcp 2>/dev/null || true
fuser -k 4200/tcp 2>/dev/null || true
pkill -f 'publisher.py --mode' 2>/dev/null || true
sleep 1

if command -v docker >/dev/null 2>&1; then
  docker start mailpit 2>/dev/null \
    || docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit 2>/dev/null \
    || echo "(Mailpit skipped — set codepulse.notification.enabled=false if SMTP unavailable)"
fi

cd "$ROOT/backend"
: >"$BACKEND_LOG"
./mvnw spring-boot:run -Dspring-boot.run.arguments=--codepulse.mode=full \
  >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "    Backend log: $BACKEND_LOG"

cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  npm install
fi
npm start &
FRONTEND_PID=$!

cd "$ROOT/challenge-publisher"
PUB_PID=""
if [[ -x .venv/bin/python ]]; then
  .venv/bin/python publisher.py --mode both --interval 20 \
    --email "${TARGET_EMAIL:-demo.user@codepulse.local}" \
    --user-id "${TARGET_USER_ID:-90002}" &
  PUB_PID=$!
else
  echo "(Publisher skipped — create venv in challenge-publisher first)"
fi

cleanup() {
  echo
  echo "Stopping full stack…"
  [[ -n "${PUB_PID:-}" ]] && kill "$PUB_PID" 2>/dev/null || true
  [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  pkill -f 'publisher.py --mode' 2>/dev/null || true
  fuser -k 8080/tcp 2>/dev/null || true
  fuser -k 4200/tcp 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Waiting for API…"
API_UP=0
for i in $(seq 1 90); do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "ERROR: backend exited early. Last log lines:"
    tail -n 40 "$BACKEND_LOG" || true
    exit 1
  fi
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/auth/login \
    -H 'Content-Type: application/json' -d '{}' || true)
  if [[ "$code" != "000" && -n "$code" ]]; then
    echo "API is up (HTTP $code)."
    API_UP=1
    break
  fi
  sleep 2
done

if [[ "$API_UP" -ne 1 ]]; then
  echo "ERROR: API did not become ready in time. Last log lines:"
  tail -n 40 "$BACKEND_LOG" || true
  exit 1
fi

echo "App: http://localhost:4200 | Mailpit: http://localhost:8025 (if Docker)"
wait
