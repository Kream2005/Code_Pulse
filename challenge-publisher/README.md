# challenge-publisher

Publishes coding-challenge JSON events for CodePulse (Postgres always; Kafka only in `full` mode).

## Modes

| `--mode` | Needs | Use with |
|----------|--------|----------|
| `http` | Python 3 only | `codepulse.mode=standalone` — API pulls `GET /api/challenges` |
| `kafka` | Kafka + `kafka-python` | `codepulse.mode=full` |
| `both` | Kafka + `kafka-python` | full stack (publish + HTTP last batch) |

Kafka imports are lazy: **HTTP-only mode does not require kafka-python**.

## Local

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt   # only needed for kafka / both

# Standalone (no Docker / broker)
./.venv/bin/python publisher.py --mode http --interval 20 \
  --email demo.user@codepulse.local --user-id 77001

# Full
./.venv/bin/python publisher.py --mode both --interval 20
```

HTTP endpoint: `http://localhost:9999/api/challenges`

See root [`README.md`](../README.md) for stack modes and demo accounts.
