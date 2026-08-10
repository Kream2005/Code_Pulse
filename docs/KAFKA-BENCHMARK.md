# Kafka vs direct — index

| Document | Content |
|----------|---------|
| [`BENCHMARK-RESULTS.md`](./BENCHMARK-RESULTS.md) | **Real measured numbers** (latest run) |
| [`benchmark-results.json`](./benchmark-results.json) | Raw machine output |
| [`WHY-KAFKA.md`](./WHY-KAFKA.md) | Why Kafka (all reasons) + when standalone is enough |
| [`APPLICATION-FUNCTIONALITIES.md`](./APPLICATION-FUNCTIONALITIES.md) | Full product & role matrix |

## Reproduce

```bash
./scripts/benchmark-messaging.py --path both --count 200 --out docs/benchmark-results.json
```

Requires API with `codepulse.mode=full` and Kafka on `:9092`.
