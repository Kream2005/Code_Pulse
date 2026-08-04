#!/usr/bin/env bash
# Start the remote bot (Kafka tunnel must already be up).
set -euo pipefail
KEY="${CTS_SSH_KEY:-$HOME/projects/cts-keys/ssh-key-2026-03-12.key}"
HOST="${CTS_SSH_HOST:-ubuntu@84.8.218.107}"

ssh -i "$KEY" "$HOST" 'sudo systemctl restart codepulse-publisher && sleep 2 && sudo systemctl status codepulse-publisher --no-pager && journalctl -u codepulse-publisher -n 20 --no-pager'
