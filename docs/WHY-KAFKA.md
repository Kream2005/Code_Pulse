# Why CodePulse uses Kafka

PostgreSQL stores the domain data in **both** run modes. Kafka is the **messaging layer** for challenge-completion events in `codepulse.mode=full`.

Measured numbers for the latest run: [`BENCHMARK-RESULTS.md`](./BENCHMARK-RESULTS.md).

---

## Reasons we chose Kafka

### 1. Decoupling the challenge source from CodePulse
The coding-challenge platform can publish events without calling CodePulse APIs synchronously. CodePulse can restart, scale, or deploy without blocking the producer.

### 2. Spike absorption / back-pressure
When many candidates finish tests at once, Kafka buffers events. The consumer processes at a sustainable rate instead of overloading the API with long synchronous HTTP batches.

**Evidence (latest bench):** publish ~**963 events/s** vs direct blocking ingest ~**42 events/s**.

### 3. Faster producer hand-off
The challenge system only waits for the broker ack (~**1 ms** p50), not for full DB + notification work (~**24 ms**/event in direct mode).

### 4. End-to-end throughput under load
For the same 200 new events, Kafka E2E (~**119 events/s**) still beat direct (~**42 events/s**) because publish and process overlap asynchronously.

### 5. Durability
Events remain on the topic if CodePulse is down. After recovery, the consumer resumes (offset / consumer group) — no lost completions.

### 6. Retries and dead-letter topic (DLT)
Failed messages can be retried (`codepulse.kafka.retry.*`) and routed to `coding-challenges-dlt` without failing the whole batch in the producer’s face.

### 7. Multiple consumers / future fan-out
Same event stream can feed notifications, analytics enrichment, audit, or other services without changing the challenge publisher.

### 8. Operational visibility
Lag, consumer groups, and integration logs make ingestion supervisable by the application admin (`/admin/logs`).

### 9. Alignment with enterprise integration
Kafka is a standard async backbone for system-to-system events in this kind of platform.

---

## When we do **not** require Kafka

`codepulse.mode=standalone` still uses **PostgreSQL**. Challenge events go through the **local Kafka binary** (same topics as full). HTTP ingest remains available if the broker is down and you switch `codepulse.kafka.enabled=false`.

| Mode | DB | Messaging | Email |
|------|----|-----------|-------|
| `standalone` | PostgreSQL | Local Kafka binary (+ HTTP publisher) | Embedded GreenMail (no Docker) |
| `full` | PostgreSQL | Kafka (+ optional Mailpit Docker) | SMTP → Mailpit |

Toggle:

```properties
codepulse.mode=full   # or standalone
```

---

## Management summary

Use Kafka in production-like environments because it **decouples**, **buffers spikes**, **acknowledges producers fast**, **survives API downtime**, and supports **retries/DLT** — backed by measured intake gains on this stack (see [`BENCHMARK-RESULTS.md`](./BENCHMARK-RESULTS.md)). Demo / work PCs run the same Kafka path with a local binary (no Docker).
