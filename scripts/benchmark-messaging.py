#!/usr/bin/env python3
"""
Benchmark CodePulse challenge ingestion: Kafka vs direct (no Kafka).

Usage (API must be running):
  # Standalone API (codepulse.mode=standalone) — direct path
  ./scripts/benchmark-messaging.py --path direct --count 200

  # Full API (codepulse.mode=full, Kafka up) — Kafka path
  ./scripts/benchmark-messaging.py --path kafka --count 200

  # Both (API must support Kafka; direct uses /coding-challenges/ingest-batch)
  ./scripts/benchmark-messaging.py --path both --count 200
"""

from __future__ import annotations

import argparse
import json
import random
import statistics
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POOL_PATH = ROOT / "challenge-publisher" / "challenges_pool.json"

FIRST = ["Alice", "Bruno", "Chloe", "David", "Emma", "Farid", "Grace", "Hugo"]
LAST = ["Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit", "Richard"]


def load_pool() -> list[dict]:
    with POOL_PATH.open(encoding="utf-8") as f:
        data = json.load(f)
    if not data:
        raise SystemExit(f"empty pool: {POOL_PATH}")
    return data


def build_event(pool: list[dict], seq: int, run_base: int) -> dict:
    challenge = random.choice(pool)
    p, n = random.choice(FIRST), random.choice(LAST)
    uid = run_base + seq
    tid = run_base + 5_000_000 + seq
    uname = f"bench.{run_base}.{seq}.{p.lower()}.{n.lower()}"
    return {
        "user": {
            "id": uid,
            "nom": n,
            "prenom": p,
            "userName": uname[:80],
            "email": f"{uname}@bench.local",
            "status": True,
        },
        "test": {
            "id": tid,
            "titre": f"{challenge['titre']} [{run_base}-{seq}]",
            "description": challenge.get("description", ""),
            "tag": challenge.get("tag", "general"),
            "duree": challenge.get("duree", 60),
            "codeUrl": challenge.get("codeUrl", ""),
            "parameter": challenge.get("parameter", False),
        },
    }


def http_json(method: str, url: str, body: dict | list | None = None, token: str | None = None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {e.code} {url}: {raw}") from e


def login(base: str, email: str, password: str) -> str:
    _, payload = http_json(
        "POST",
        f"{base}/auth/login",
        {"email": email, "password": password},
    )
    token = (
        (payload or {}).get("token")
        or (payload or {}).get("accessToken")
        or (payload or {}).get("access_token")
    )
    if not token:
        raise SystemExit(f"login failed — unexpected payload: {payload}")
    return str(token)


def count_challenges(base: str, token: str) -> int:
    _, n = http_json("GET", f"{base}/coding-challenges/count-coding-challenges", token=token)
    return int(n)


def count_notifications(base: str, token: str) -> int:
    _, n = http_json("GET", f"{base}/notifications/count", token=token)
    return int(n)


def bench_direct(base: str, token: str, events: list[dict]) -> dict:
    before_c = count_challenges(base, token)
    before_n = count_notifications(base, token)
    t0 = time.perf_counter()
    status, payload = http_json(
        "POST",
        f"{base}/coding-challenges/ingest-batch",
        events,
        token=token,
    )
    elapsed = time.perf_counter() - t0
    after_c = count_challenges(base, token)
    after_n = count_notifications(base, token)
    processed = int((payload or {}).get("processed", 0))
    return {
        "path": "direct",
        "http_status": status,
        "requested": len(events),
        "processed": processed,
        "challenge_delta": after_c - before_c,
        "notification_delta": after_n - before_n,
        "elapsed_s": round(elapsed, 4),
        "throughput_events_per_s": round(len(events) / elapsed, 2) if elapsed else 0,
        "avg_latency_ms": round((elapsed / len(events)) * 1000, 2) if events else 0,
    }


def bench_kafka(bootstrap: str, topic: str, base: str, token: str, events: list[dict]) -> dict:
    from kafka import KafkaProducer

    producer = KafkaProducer(
        bootstrap_servers=bootstrap.split(","),
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
        acks="all",
        retries=3,
    )
    before_c = count_challenges(base, token)
    before_n = count_notifications(base, token)
    latencies_ms: list[float] = []
    t0 = time.perf_counter()
    for event in events:
        key = str(event["test"]["id"])
        send_t0 = time.perf_counter()
        fut = producer.send(topic, key=key, value=event)
        fut.get(timeout=30)
        latencies_ms.append((time.perf_counter() - send_t0) * 1000)
    producer.flush()
    publish_elapsed = time.perf_counter() - t0

    deadline = time.time() + 120
    after_n = before_n
    after_c = before_c
    while time.time() < deadline:
        after_n = count_notifications(base, token)
        after_c = count_challenges(base, token)
        if after_n - before_n >= len(events):
            break
        time.sleep(0.2)
    e2e = time.perf_counter() - t0
    consumed = after_n - before_n
    return {
        "path": "kafka",
        "requested": len(events),
        "published": len(events),
        "challenge_delta": after_c - before_c,
        "notification_delta": consumed,
        "publish_elapsed_s": round(publish_elapsed, 4),
        "e2e_elapsed_s": round(e2e, 4),
        "publish_throughput_events_per_s": round(len(events) / publish_elapsed, 2) if publish_elapsed else 0,
        "e2e_throughput_events_per_s": round(consumed / e2e, 2) if e2e and consumed else 0,
        "publish_p50_ms": round(statistics.median(latencies_ms), 2) if latencies_ms else 0,
        "publish_p95_ms": round(statistics.quantiles(latencies_ms, n=20)[18], 2)
        if len(latencies_ms) >= 20
        else round(max(latencies_ms), 2)
        if latencies_ms
        else 0,
        "fully_consumed": consumed >= len(events),
    }


def main() -> int:
    p = argparse.ArgumentParser(description="CodePulse Kafka vs direct ingest benchmark")
    p.add_argument("--base-url", default="http://localhost:8080")
    p.add_argument("--email", default="admin@codepulse.local")
    p.add_argument("--password", default="Admin1234!")
    p.add_argument("--count", type=int, default=200)
    p.add_argument("--path", choices=("direct", "kafka", "both"), default="both")
    p.add_argument("--bootstrap", default="localhost:9092")
    p.add_argument("--topic", default="coding-challenges")
    p.add_argument("--out", default=str(ROOT / "docs" / "benchmark-results.json"))
    args = p.parse_args()

    pool = load_pool()
    token = login(args.base_url, args.email, args.password)
    run_base = int(time.time()) % 1_000_000_000
    results: list[dict] = []
    meta = {
        "date": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "count": args.count,
        "run_base": run_base,
        "base_url": args.base_url,
    }

    if args.path in ("direct", "both"):
        events = [build_event(pool, i, run_base) for i in range(args.count)]
        print(f"[direct] ingesting {args.count} events…", flush=True)
        results.append(bench_direct(args.base_url, token, events))
        print(json.dumps(results[-1], indent=2), flush=True)

    if args.path in ("kafka", "both"):
        events = [build_event(pool, args.count + i, run_base) for i in range(args.count)]
        print(f"[kafka] publishing {args.count} events…", flush=True)
        results.append(bench_kafka(args.bootstrap, args.topic, args.base_url, token, events))
        print(json.dumps(results[-1], indent=2), flush=True)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"meta": meta, "results": results}, indent=2), encoding="utf-8")
    print(f"\nWrote {out}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
