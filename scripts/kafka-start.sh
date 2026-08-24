#!/usr/bin/env bash
# Start a local Kafka binary (KRaft, no ZooKeeper, no Docker).
# Set KAFKA_HOME to the unpacked Kafka folder (the one that contains bin/ and config/).
set -euo pipefail

resolve_java_home() {
  local candidates=(
    "${CODEPULSE_JAVA_HOME:-}"
    "${JAVA_HOME:-}"
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
  return 1
}

resolve_kafka_home() {
  local candidates=(
    "${KAFKA_HOME:-}"
    "/opt/kafka"
    "$HOME/kafka"
    "$HOME/opt/kafka"
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ -n "$c" && -x "$c/bin/kafka-server-start.sh" ]]; then
      echo "$c"
      return 0
    fi
  done
  return 1
}

resolve_kafka_config() {
  local home="$1"
  if [[ -f "$home/config/server.properties" ]] \
      && grep -qE '^process.roles=' "$home/config/server.properties"; then
    echo "$home/config/server.properties"
    return 0
  fi
  if [[ -f "$home/config/kraft/server.properties" ]]; then
    echo "$home/config/kraft/server.properties"
    return 0
  fi
  if [[ -f "$home/config/server.properties" ]]; then
    echo "$home/config/server.properties"
    return 0
  fi
  return 1
}

JAVA_HOME="$(resolve_java_home)" || {
  echo "ERROR: JDK 17+ (21 recommended) not found. Set JAVA_HOME or CODEPULSE_JAVA_HOME." >&2
  exit 1
}
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

KAFKA_HOME="$(resolve_kafka_home)" || {
  echo "ERROR: Kafka binary not found." >&2
  echo "Unpack the Apache Kafka download and set:" >&2
  echo "  export KAFKA_HOME=/path/to/kafka" >&2
  echo "The folder must contain bin/kafka-server-start.sh" >&2
  exit 1
}
export KAFKA_HOME
export PATH="$KAFKA_HOME/bin:$PATH"

CONFIG="$(resolve_kafka_config "$KAFKA_HOME")" || {
  echo "ERROR: No Kafka server.properties under $KAFKA_HOME/config" >&2
  exit 1
}

LOG_DIRS=$(grep -E '^log.dirs=' "$CONFIG" | tail -1 | cut -d= -f2- || true)
LOG_DIRS="${LOG_DIRS:-$KAFKA_HOME/kraft-combined-logs}"
mkdir -p "$LOG_DIRS"

if [[ -f "$KAFKA_HOME/config/log4j2.yaml" ]]; then
  export KAFKA_LOG4J_OPTS="-Dlog4j.configurationFile=file:$KAFKA_HOME/config/log4j2.yaml"
fi

if [ ! -f "$LOG_DIRS/meta.properties" ]; then
  echo "Formatting Kafka storage at $LOG_DIRS"
  CLUSTER_ID=$("$KAFKA_HOME/bin/kafka-storage.sh" random-uuid)
  if ! "$KAFKA_HOME/bin/kafka-storage.sh" format --standalone -t "$CLUSTER_ID" -c "$CONFIG" 2>/dev/null; then
    "$KAFKA_HOME/bin/kafka-storage.sh" format -t "$CLUSTER_ID" -c "$CONFIG"
  fi
fi

if ss -tln 2>/dev/null | grep -q ':9092' || netstat -tln 2>/dev/null | grep -q ':9092'; then
  echo "Kafka already listening on :9092"
else
  "$KAFKA_HOME/bin/kafka-server-start.sh" -daemon "$CONFIG"
  for _ in $(seq 1 40); do
    if ss -tln 2>/dev/null | grep -q ':9092' || netstat -tln 2>/dev/null | grep -q ':9092'; then
      break
    fi
    sleep 1
  done
fi

if ! ss -tln 2>/dev/null | grep -q ':9092' && ! netstat -tln 2>/dev/null | grep -q ':9092'; then
  echo "Kafka failed to start on :9092" >&2
  echo "Check logs under $LOG_DIRS and $KAFKA_HOME/logs" >&2
  exit 1
fi

"$KAFKA_HOME/bin/kafka-topics.sh" --create --if-not-exists --bootstrap-server localhost:9092 \
  --topic coding-challenges --partitions 3 --replication-factor 1 >/dev/null
"$KAFKA_HOME/bin/kafka-topics.sh" --create --if-not-exists --bootstrap-server localhost:9092 \
  --topic coding-challenges-dlt --partitions 1 --replication-factor 1 >/dev/null

echo "Kafka is up (topics ready)  KAFKA_HOME=$KAFKA_HOME"
