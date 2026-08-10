#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/jdk-21.0.6-oracle-x64}"
export KAFKA_HOME="${KAFKA_HOME:-/opt/kafka}"
export PATH="$JAVA_HOME/bin:$KAFKA_HOME/bin:$PATH"

LOG_DIRS=$(grep -E '^log.dirs=' "$KAFKA_HOME/config/kraft/server.properties" | cut -d= -f2)
LOG_DIRS="${LOG_DIRS:-/home/kream2005/kafka-data}"
mkdir -p "$LOG_DIRS"

if [ ! -f "$LOG_DIRS/meta.properties" ]; then
  echo "Formatting Kafka storage at $LOG_DIRS"
  CLUSTER_ID=$("$KAFKA_HOME/bin/kafka-storage.sh" random-uuid)
  "$KAFKA_HOME/bin/kafka-storage.sh" format -t "$CLUSTER_ID" -c "$KAFKA_HOME/config/kraft/server.properties"
fi

if ss -tln | grep -q ':9092'; then
  echo "Kafka already listening on :9092"
else
  "$KAFKA_HOME/bin/kafka-server-start.sh" -daemon "$KAFKA_HOME/config/kraft/server.properties"
  for _ in $(seq 1 40); do
    if ss -tln | grep -q ':9092'; then
      break
    fi
    sleep 1
  done
fi

if ! ss -tln | grep -q ':9092'; then
  echo "Kafka failed to start on :9092" >&2
  exit 1
fi

"$KAFKA_HOME/bin/kafka-topics.sh" --create --if-not-exists --bootstrap-server localhost:9092 \
  --topic coding-challenges --partitions 3 --replication-factor 1 >/dev/null
"$KAFKA_HOME/bin/kafka-topics.sh" --create --if-not-exists --bootstrap-server localhost:9092 \
  --topic coding-challenges-dlt --partitions 1 --replication-factor 1 >/dev/null

echo "Kafka is up (topics ready)"
