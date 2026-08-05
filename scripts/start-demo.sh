#!/usr/bin/env bash
# Standalone mode: Postgres + Java + Node (no Kafka, Mailpit, or Docker).
# Toggle: codepulse.mode=standalone in backend application.properties
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_LOG="${TMPDIR:-/tmp}/codepulse-backend-standalone.log"
RESOURCES="$ROOT/backend/src/main/resources"

# Prefer JDK 21 even if the shell already exported an older JAVA_HOME (e.g. 11).
resolve_java_home() {
  local candidates=(
    "${CODEPULSE_JAVA_HOME:-}"
    "/usr/lib/jvm/jdk-21.0.6-oracle-x64"
    "/usr/lib/jvm/java-21-openjdk-amd64"
    "/usr/lib/jvm/java-21-openjdk"
    "/usr/lib/jvm/jdk-21"
    "/opt/homebrew/opt/openjdk@21"
    "/usr/local/opt/openjdk@21"
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ -n "$c" && -x "$c/bin/java" ]]; then
      echo "$c"
      return 0
    fi
  done
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local mac
    mac="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "$mac" && -x "$mac/bin/java" ]]; then
      echo "$mac"
      return 0
    fi
  fi
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]] \
      && "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
    echo "$JAVA_HOME"
    return 0
  fi
  return 1
}

ensure_local_config() {
  if [[ ! -f "$RESOURCES/application.properties" ]]; then
    echo "==> Creating backend/src/main/resources/application.properties from example"
    cp "$RESOURCES/application.properties.example" "$RESOURCES/application.properties"
    echo "    Edit spring.datasource.* if your Postgres user/password differ."
    echo "    Default DB (scripts/create-db.sql): codepulse / codepulse / codepulse"
  fi

  if [[ ! -f "$RESOURCES/private.key" || ! -f "$RESOURCES/public.key" ]]; then
    if ! command -v openssl >/dev/null 2>&1; then
      echo "ERROR: JWT keys missing and openssl is not installed."
      echo "Install openssl, or place private.key + public.key in backend/src/main/resources/"
      exit 1
    fi
    echo "==> Generating demo JWT RSA keys (local only, gitignored)"
    openssl genrsa -out "$RESOURCES/private.key" 2048 >/dev/null 2>&1
    openssl rsa -in "$RESOURCES/private.key" -pubout -out "$RESOURCES/public.key" >/dev/null 2>&1
    chmod 600 "$RESOURCES/private.key"
  fi
}

ensure_node() {
  export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
  if [ -s "$NVM_DIR/nvm.sh" ]; then
    # shellcheck disable=SC1090
    . "$NVM_DIR/nvm.sh"
    nvm use 25 >/dev/null 2>&1 || nvm use --lts >/dev/null 2>&1 || true
  fi
  if ! command -v node >/dev/null 2>&1; then
    echo "ERROR: Node.js is required (25+ recommended)."
    echo "Install Node, or load nvm (nvm install 25 && nvm use 25)."
    exit 1
  fi
  local major
  major="$(node -p "process.versions.node.split('.')[0]" 2>/dev/null || echo 0)"
  if [[ "$major" -lt 20 ]]; then
    echo "ERROR: Node.js $major is too old. Need Node 20+ (25+ recommended)."
    exit 1
  fi
  if [[ "$major" -lt 25 ]]; then
    echo "WARN: Node.js $major detected; project engines ask for 25+. Continuing anyway."
  fi
  echo "    Node: $(node -v) ($(command -v node))"
}

JAVA_HOME="$(resolve_java_home)" || {
  echo "ERROR: JDK 21 is required but was not found."
  echo "Install JDK 21, or set CODEPULSE_JAVA_HOME=/path/to/jdk-21"
  exit 1
}
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "    Java: $JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -1))"

ensure_local_config
ensure_node

echo "==> CodePulse STANDALONE mode (Postgres + HTTP publisher, Kafka/mail off)"
echo "    Requires: PostgreSQL :5432 (scripts/create-db.sql once)"
echo "    Seed: challenges, candidates, questions, feedbacks, inbox notifs, password resets"
echo "    App:  http://localhost:4200"
echo "    API:  http://localhost:8080"
echo "    Publisher HTTP: http://localhost:9999/api/challenges"
echo "    Accounts:"
echo "      USER:                  demo.user@codepulse.local / Demo1234!"
echo "      ADMIN_CODING_CHALLENGE: challenge.admin@codepulse.local / Challenge1234!"
echo "      MANAGER_RH:            manager.rh@codepulse.local / Manager1234!"
echo "      ADMIN_CODEPULSE:       admin@codepulse.local / Admin1234!"
echo

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
for i in $(seq 1 90); do
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
