# Benchmark results — Kafka vs direct (real run)

**Date:** 2026-08-02  
**Environment:** `codepulse.mode=full`, PostgreSQL local, Kafka `localhost:9092`  
**Workload:** 200 coding-challenge completion events (unique IDs per run)  
**Script:** `scripts/benchmark-messaging.py --path both --count 200`  
**Raw JSON:** [`benchmark-results.json`](./benchmark-results.json)

Same business logic in both paths (`processIncomingChallenge` → challenge + user + notification in Postgres). Only the transport differs.

---

## Results

| Path | What was measured | Wall time | Throughput | Latency / notes |
|------|-------------------|-----------|------------|-----------------|
| **Direct** | HTTP `ingest-batch` until all 200 processed | **4.79 s** | **41.8 events/s** | avg **23.9 ms**/event · +200 challenges · +200 notifications |
| **Kafka publish** | Broker ack for all 200 writes | **0.21 s** | **963 events/s** | p50 **1.0 ms** · p95 **1.37 ms** |
| **Kafka E2E** | Publish → consumer → Postgres (+200 notifications) | **1.68 s** | **119.0 events/s** | `fully_consumed: true` |

### Ratios (this run)

| Comparison | Factor |
|------------|--------|
| Kafka publish vs direct throughput | **~23×** faster hand-off (963 / 42) |
| Kafka E2E vs direct throughput | **~2.8×** faster full pipeline (119 / 42) |
| Direct wall time vs Kafka E2E | Direct took **~2.8×** longer (4.79 s / 1.68 s) |

---

## How to re-run

```bash
# API must be up with Kafka (codepulse.mode=full)
./scripts/benchmark-messaging.py --path both --count 200 --out docs/benchmark-results.json
```

Then update this file with the new JSON numbers.

See also: [`WHY-KAFKA.md`](./WHY-KAFKA.md) (reasons) · [`APPLICATION-FUNCTIONALITIES.md`](./APPLICATION-FUNCTIONALITIES.md) (product / roles).
