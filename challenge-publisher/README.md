# challenge-publisher

Publishes coding-challenge JSON events for CodePulse (Postgres always; Kafka in both modes).

## Modes

| `--mode` | Needs | Use with |
|----------|--------|----------|
| `http` | Python 3 only | Kafka down / HTTP sync only |
| `kafka` | Kafka + `kafka-python` | `standalone` or `full` |
| `both` | Kafka + `kafka-python` | default for demo and full (publish + HTTP last batch) |

Kafka imports are lazy: **HTTP-only mode does not require kafka-python**.

## Local

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt   # only needed for kafka / both

# Standalone / demo (Kafka binary, no Docker)
./.venv/bin/python publisher.py --mode both --interval 20 \
  --email demo.user@codepulse.local --user-id 77001

# Full
./.venv/bin/python publisher.py --mode both --interval 20
```

HTTP endpoint: `http://localhost:9999/api/challenges`

See root [`README.md`](../README.md) for stack modes and demo accounts.
