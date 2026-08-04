#!/usr/bin/env bash
set -euo pipefail
KEY="${CTS_SSH_KEY:-$HOME/projects/cts-keys/ssh-key-2026-03-12.key}"
HOST="${CTS_SSH_HOST:-ubuntu@84.8.218.107}"

echo "=== remote bot status ==="
ssh -i "$KEY" "$HOST" 'systemctl is-active codepulse-publisher; curl -s http://127.0.0.1:9999/api/challenges | python3 -m json.tool | head -25; journalctl -u codepulse-publisher -n 15 --no-pager'

echo "=== local DB counts (backend must be running) ==="
PGPASSWORD="${PGPASSWORD:-Aa123890}" psql -h localhost -U codepulse -d codepulse -c \
  'select (select count(*) from coding_challenge) challenges, (select count(*) from notification) notifs;'
