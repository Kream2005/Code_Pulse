#!/usr/bin/env bash
# From your PC: expose local Kafka to the OCI server via reverse SSH tunnel.
# On the server, bot uses 127.0.0.1:9092 → this tunnel → your PC Kafka.
set -euo pipefail
KEY="${CTS_SSH_KEY:-$HOME/projects/cts-keys/ssh-key-2026-03-12.key}"
HOST="${CTS_SSH_HOST:-ubuntu@84.8.218.107}"

if ! ss -tln | grep -q ':9092'; then
  echo "Start Kafka first: ./scripts/kafka-start.sh" >&2
  exit 1
fi

echo "Tunnel: $HOST:127.0.0.1:9092  -->  this-PC:127.0.0.1:9092"
echo "Keep this terminal open. Then on the server: sudo systemctl restart codepulse-publisher"
exec ssh -i "$KEY" -N -o ExitOnForwardFailure=yes -R 9092:127.0.0.1:9092 "$HOST"
