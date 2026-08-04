#!/usr/bin/env bash
# Standalone mode: Postgres + Java + Node (no Kafka, Mailpit, or Docker).
# Toggle: codepulse.mode=standalone in backend application.properties
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_LOG="${TMPDIR:-/tmp}/codepulse-backend-standalone.log"

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

echo "==> CodePulse STANDALONE mode (Postgres + HTTP publisher, Kafka/mail off)"
echo "    Requires: PostgreSQL :5432"
echo "    Seed: ~48 challenges, ~28 candidates, ~16 questions, ~36 feedbacks,"
echo "          ~40 inbox notifs for demo.user, ~18 password-reset requests"
echo "    App:  http://localhost:4200"
echo "    API:  http://localhost:8080"
echo "    Publisher HTTP: http://localhost:9999/api/challenges"
echo "    Admin: admin@codepulse.local / Admin1234!"
echo "    User:  demo.user@codepulse.local / Demo1234!"
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
fuser -k 9999/tcp 2>/dev/null || true
pkill -f 'publisher.py --mode' 2>/dev/null || true
sleep 1

cd "$ROOT/challenge-publisher"
PUB_PID=""
if [[ -x .venv/bin/python ]]; then
  .venv/bin/python publisher.py --mode http --interval 20 --batch-size 5 \
    --email "${TARGET_EMAIL:-demo.user@codepulse.local}" \
    --user-id "${TARGET_USER_ID:-90002}" &
  PUB_PID=$!
else
  python3 publisher.py --mode http --interval 20 --batch-size 5 \
    --email "${TARGET_EMAIL:-demo.user@codepulse.local}" \
    --user-id "${TARGET_USER_ID:-90002}" &
  PUB_PID=$!
fi

cd "$ROOT/backend"
: >"$BACKEND_LOG"
./mvnw spring-boot:run -Dspring-boot.run.arguments=--codepulse.mode=standalone \
  >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "    Backend log: $BACKEND_LOG"

cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  npm install
fi
npm start &
FRONTEND_PID=$!

cleanup() {
  echo
  echo "Stopping standalone stack…"
  [[ -n "${PUB_PID:-}" ]] && kill "$PUB_PID" 2>/dev/null || true
  [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  pkill -f 'publisher.py --mode' 2>/dev/null || true
  fuser -k 8080/tcp 2>/dev/null || true
  fuser -k 4200/tcp 2>/dev/null || true
  fuser -k 9999/tcp 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Waiting for API…"
API_UP=0
for i in $(seq 1 60); do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "ERROR: backend exited early. Last log lines:"
    tail -n 40 "$BACKEND_LOG" || true
    exit 1
  fi
  if curl -sf -o /dev/null -X POST http://localhost:8080/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"admin@codepulse.local","password":"Admin1234!"}'; then
    echo "API is up."
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

echo "Frontend at http://localhost:4200 — leave this terminal open."
wait
