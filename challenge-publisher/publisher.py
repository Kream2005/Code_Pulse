#!/usr/bin/env python3
"""
Pushes random CodingChallengeEvent JSON to Kafka (and optionally serves the same
shape over HTTP for Spring's /coding-challenges/synchroniser pull path).
"""

from __future__ import annotations

import argparse
import json
import os
import random
import signal
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent
POOL_PATH = ROOT / "challenges_pool.json"

FIRST_NAMES = [
    "Alice", "Bruno", "Chloe", "David", "Emma", "Farid", "Grace", "Hugo",
    "Ines", "Jules", "Karim", "Lea", "Marie", "Noah", "Omar", "Paul",
]
LAST_NAMES = [
    "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Petit", "Richard",
    "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel",
]

_latest_batch: list[dict[str, Any]] = []
_stop = threading.Event()


def load_pool() -> list[dict[str, Any]]:
    with POOL_PATH.open(encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list) or not data:
        raise SystemExit(f"empty challenge pool: {POOL_PATH}")
    return data


def slug(s: str) -> str:
    return "".join(ch if ch.isalnum() else "." for ch in s.lower()).strip(".")


def build_event(
    pool: list[dict[str, Any]],
    *,
    user_id: int | None = None,
    test_id: int | None = None,
    email: str | None = None,
    prenom: str | None = None,
    nom: str | None = None,
    username: str | None = None,
) -> dict[str, Any]:
    challenge = random.choice(pool)
    p = prenom or random.choice(FIRST_NAMES)
    n = nom or random.choice(LAST_NAMES)
    uid = user_id or random.randint(1000, 999_999)
    tid = test_id or random.randint(10_000, 9_999_999)
    base = f"{slug(p)}.{slug(n)}"
    uname = username or f"{base}.{uid}"
    mail = email or f"{uname}@example.com"
    return {
        "user": {
            "id": uid,
            "nom": n,
            "prenom": p,
            "userName": uname,
            "email": mail,
            "status": True,
        },
        "test": {
            "id": tid,
            "titre": challenge["titre"],
            "description": challenge.get("description", ""),
            "tag": challenge.get("tag", "general"),
            "duree": challenge.get("duree", 60),
            "codeUrl": challenge.get("codeUrl", ""),
            "parameter": challenge.get("parameter", False),
        },
    }


def make_producer(bootstrap: str) -> Any:
    from kafka import KafkaProducer

    return KafkaProducer(
        bootstrap_servers=bootstrap.split(","),
        key_serializer=lambda k: k.encode("utf-8"),
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
        acks="all",
        retries=3,
        linger_ms=50,
    )


def publish_one(producer: Any, topic: str, event: dict[str, Any]) -> None:
    key = str(event["test"]["id"])
    future = producer.send(topic, key=key, value=event)
    meta = future.get(timeout=15)
    print(
        f"[kafka] sent test={key} titre={event['test']['titre']!r} "
        f"user={event['user']['email']} -> {meta.topic}:{meta.partition}@{meta.offset}",
        flush=True,
    )


class ChallengeApiHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args: Any) -> None:
        print(f"[http] {self.address_string()} {fmt % args}", flush=True)

    def do_GET(self) -> None:  # noqa: N802
        if self.path.rstrip("/") not in ("/api/challenges", "/api/challenges/"):
            self.send_error(404, "use GET /api/challenges")
            return
        body = json.dumps(_latest_batch or [], ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def start_http(port: int) -> ThreadingHTTPServer:
    server = ThreadingHTTPServer(("0.0.0.0", port), ChallengeApiHandler)
    thread = threading.Thread(target=server.serve_forever, name="http", daemon=True)
    thread.start()
    print(f"[http] listening on http://0.0.0.0:{port}/api/challenges", flush=True)
    return server


def wait_for_broker(bootstrap: str, timeout_s: int = 60) -> Any:
    from kafka.errors import NoBrokersAvailable

    deadline = time.time() + timeout_s
    last_err: Exception | None = None
    while time.time() < deadline and not _stop.is_set():
        producer = None
        try:
            producer = make_producer(bootstrap)
            producer.partitions_for("coding-challenges")
            print(f"[kafka] connected to {bootstrap}", flush=True)
            return producer
        except NoBrokersAvailable as exc:
            last_err = exc
            print(f"[kafka] broker not ready ({bootstrap}), retrying...", flush=True)
        except Exception as exc:  # noqa: BLE001
            last_err = exc
            print(f"[kafka] connect error: {exc}", flush=True)
        if producer is not None:
            try:
                producer.close()
            except Exception:  # noqa: BLE001
                pass
        time.sleep(2)
    raise SystemExit(f"could not reach Kafka at {bootstrap}: {last_err}")


def run_loop(
    producer: Any,
    topic: str,
    pool: list[dict[str, Any]],
    interval: float,
    batch_size: int,
    *,
    target_email: str | None = None,
    target_user_id: int | None = None,
    target_prenom: str | None = None,
    target_nom: str | None = None,
    target_username: str | None = None,
) -> None:
    global _latest_batch
    while not _stop.is_set():
        batch = [
            build_event(
                pool,
                user_id=target_user_id,
                email=target_email,
                prenom=target_prenom,
                nom=target_nom,
                username=target_username,
            )
            for _ in range(batch_size)
        ]
        _latest_batch = batch
        for event in batch:
            try:
                publish_one(producer, topic, event)
            except Exception as exc:  # noqa: BLE001
                print(f"[kafka] publish failed: {exc}", flush=True)
        producer.flush()
        _stop.wait(interval)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="CodePulse coding-challenge Kafka publisher")
    p.add_argument("--bootstrap", default=os.getenv("KAFKA_BOOTSTRAP", "localhost:9092"))
    p.add_argument("--topic", default=os.getenv("KAFKA_TOPIC", "coding-challenges"))
    p.add_argument("--interval", type=float, default=float(os.getenv("INTERVAL_SECONDS", "15")))
    p.add_argument("--batch-size", type=int, default=int(os.getenv("BATCH_SIZE", "1")))
    p.add_argument("--http-port", type=int, default=int(os.getenv("HTTP_PORT", "9999")))
    p.add_argument(
        "--mode",
        choices=("kafka", "http", "both"),
        default=os.getenv("MODE", "both"),
        help="kafka=publish only, http=API only, both=publish + serve last batch",
    )
    p.add_argument("--once", action="store_true", help="send one batch then exit")
    p.add_argument(
        "--email",
        default=os.getenv("TARGET_EMAIL"),
        help="Always publish for this user email (Kafka -> API inbox + notification email)",
    )
    p.add_argument(
        "--user-id",
        type=int,
        default=int(os.getenv("TARGET_USER_ID", "0")) or None,
        help="Stable external user id",
    )
    p.add_argument("--prenom", default=os.getenv("TARGET_PRENOM", "Abdelkarim"))
    p.add_argument("--nom", default=os.getenv("TARGET_NOM", "Ameur"))
    p.add_argument("--username", default=os.getenv("TARGET_USERNAME", "abdelkarim.ameur"))
    return p.parse_args()


def main() -> int:
    args = parse_args()
    pool = load_pool()
    target_kwargs = {}
    if args.email:
        target_kwargs = {
            "target_email": args.email,
            "target_user_id": args.user_id,
            "target_prenom": args.prenom,
            "target_nom": args.nom,
            "target_username": args.username,
        }
        print(f"[target] email={args.email} user_id={args.user_id}", flush=True)

    def _handle_signal(signum: int, _frame: Any) -> None:
        print(f"stopping (signal {signum})", flush=True)
        _stop.set()

    signal.signal(signal.SIGINT, _handle_signal)
    signal.signal(signal.SIGTERM, _handle_signal)

    http_server: ThreadingHTTPServer | None = None
    if args.mode in ("http", "both"):
        # seed something for the first GET
        global _latest_batch
        _latest_batch = [
            build_event(
                pool,
                user_id=args.user_id,
                email=args.email,
                prenom=args.prenom if args.email else None,
                nom=args.nom if args.email else None,
                username=args.username if args.email else None,
            )
            for _ in range(max(1, args.batch_size))
        ]
        http_server = start_http(args.http_port)

    producer: Any | None = None
    if args.mode in ("kafka", "both"):
        producer = wait_for_broker(args.bootstrap)
        if args.once:
            batch = [
                build_event(
                    pool,
                    user_id=args.user_id,
                    email=args.email,
                    prenom=args.prenom if args.email else None,
                    nom=args.nom if args.email else None,
                    username=args.username if args.email else None,
                )
                for _ in range(max(1, args.batch_size))
            ]
            _latest_batch = batch
            for event in batch:
                publish_one(producer, args.topic, event)
            producer.flush()
            print("[done] --once finished", flush=True)
            if http_server:
                http_server.shutdown()
            producer.close()
            return 0
        run_loop(
            producer,
            args.topic,
            pool,
            args.interval,
            max(1, args.batch_size),
            **target_kwargs,
        )
    else:
        # http-only: refresh batch on an interval without Kafka
        while not _stop.is_set():
            _latest_batch = [
                build_event(
                    pool,
                    user_id=args.user_id,
                    email=args.email,
                    prenom=args.prenom if args.email else None,
                    nom=args.nom if args.email else None,
                    username=args.username if args.email else None,
                )
                for _ in range(max(1, args.batch_size))
            ]
            print(f"[http] refreshed batch size={len(_latest_batch)}", flush=True)
            _stop.wait(args.interval)

    if http_server:
        http_server.shutdown()
    if producer:
        producer.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
