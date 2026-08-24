#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-${CODEPULSE_JAVA_HOME:-/usr/lib/jvm/jdk-21.0.6-oracle-x64}}"
export KAFKA_HOME="${KAFKA_HOME:-/opt/kafka}"
export PATH="$JAVA_HOME/bin:$KAFKA_HOME/bin:$PATH"

if systemctl list-unit-files 2>/dev/null | grep -q '^kafka.service'; then
  sudo systemctl stop kafka || true
fi

if [[ -x "$KAFKA_HOME/bin/kafka-server-stop.sh" ]]; then
  "$KAFKA_HOME/bin/kafka-server-stop.sh" || true
fi
echo "Kafka stop requested"
