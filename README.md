# CodePulse

Feedback platform for coding challenges — collect notifications after a challenge, submit feedback, and analyse results by role.

| Layer | Stack |
|-------|-------|
| Backend | Spring Boot, Java 21, JWT, JPA |
| Frontend | React 19, Vite 8, Tailwind 4 |
| Database | **PostgreSQL** (required in both modes) |
| Messaging | Kafka in `full` mode; HTTP direct ingest in `standalone` |
| Email (optional) | Mailpit / SMTP via `codepulse.notification.enabled` |

**Node.js:** **25+** (`engines.node >= 25`).

---

## Runtime toggle (one property)

Copy the example config once, then edit the mode:

```bash
cp backend/src/main/resources/application.properties.example \
   backend/src/main/resources/application.properties
```

```properties
codepulse.mode=standalone   # or: full
```

| Value | Needs | Challenge ingestion |
|-------|--------|---------------------|
| `standalone` | PostgreSQL + JDK + Node | HTTP publisher → direct process (no Kafka) |
| `full` | PostgreSQL + Kafka (+ optional Mailpit) | Kafka consumer (and/or HTTP sync → Kafka) |

Independent switches (still honored inside each mode):

- `codepulse.kafka.enabled`
- `codepulse.notification.enabled`
- `codepulse.external-api.enabled`

---

## Quick start

### Linux

```bash
nvm use 25
./scripts/start-demo.sh    # standalone (Postgres, HTTP publisher, no Kafka)
./scripts/start-full.sh    # Postgres + Kafka + optional Mailpit
```

### Windows

```bat
cd frontend && npm install && cd ..
scripts\start-demo.bat
```

| URL | |
|-----|--|
| UI | http://localhost:4200 |
| API | http://localhost:8080 |
| Publisher HTTP | http://localhost:9999/api/challenges |
| Mailpit (full, optional) | http://localhost:8025 |

---

## Demo accounts (all roles)

| Role | Email | Password | Main area |
|------|-------|----------|-----------|
| Utilisateur (candidat) | `demo.user@codepulse.local` | `Demo1234!` | Inbox, feedback form, my feedbacks |
| Admin Coding Challenge | `challenge.admin@codepulse.local` | `Challenge1234!` | Challenges, notifications, feedbacks |
| Manager / RH | `manager.rh@codepulse.local` | `Manager1234!` | Feedbacks, analytics dashboard |
| Admin application | `admin@codepulse.local` | `Admin1234!` | Users, questions, logs, integration, all admin |

Accounts are seeded automatically (`RoleAccountsSeeder`).

---

## Roles & pages

| Role | What they do | Pages |
|------|----------------|-------|
| **USER** | Receive notification after a challenge, submit feedback | `/inbox`, `/feedback/form`, `/my-feedback`, `/profile` |
| **ADMIN_CODING_CHALLENGE** | Sync / archive challenges, consult related feedbacks | `/admin`, `/admin/challenges`, `/admin/notifications`, `/admin/feedbacks` |
| **MANAGER_RH** | Analyse returns, dashboards & indicators | `/admin`, `/admin/feedbacks`, `/admin/analytics` |
| **ADMIN_CODEPULSE** | Questions, users, password resets, logs, supervise stack | `/admin/*` (full admin) |

Invite-only auth: candidates are provisioned from challenge events (no public register).

---

## Dashboard KPIs

| Role | API | Indicators |
|------|-----|------------|
| USER | `GET /analytics/dashboard/user` | notifications, pending, feedbacks submitted |
| Admin Coding Challenge | `GET /analytics/dashboard/challenge-admin` | active/archived challenges, notifications, feedbacks, avg score |
| Manager RH | `GET /analytics/dashboard/manager` | participation %, challenges, feedbacks, avg score, pending, tags |
| Admin app | `GET /analytics/dashboard/app-admin` | staff, candidates, questions, pending resets, error logs, Kafka/mail flags, challenges, feedbacks |

Implementation: `AnalyticsService` / `AnalyticsServiceImp` + admin/inbox UI strips.

---

## Challenge publisher

`challenge-publisher/` — works in both modes:

```bash
cd challenge-publisher
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt   # needed for kafka / both

# Standalone (no Kafka)
python publisher.py --mode http --interval 20

# Full
python publisher.py --mode both --interval 20
```

| `--mode` | Use with |
|----------|----------|
| `http` | `standalone` — API sync pulls `GET /api/challenges` |
| `kafka` / `both` | `full` |

---

## Benchmark (Kafka vs direct)

```bash
./scripts/benchmark-messaging.py --path both --count 200
```

| Doc | Content |
|-----|---------|
| [`docs/BENCHMARK-RESULTS.md`](docs/BENCHMARK-RESULTS.md) | Latest **real** numbers |
| [`docs/benchmark-results.json`](docs/benchmark-results.json) | Raw JSON |
| [`docs/WHY-KAFKA.md`](docs/WHY-KAFKA.md) | Why Kafka (all reasons) |
| [`docs/KAFKA-BENCHMARK.md`](docs/KAFKA-BENCHMARK.md) | How to re-run |

Latest summary (200 events): direct ~**42 evt/s** · Kafka publish ~**963 evt/s** · Kafka E2E ~**119 evt/s**.

---

## Documentation index

| Doc | Description |
|-----|-------------|
| [`docs/APPLICATION-FUNCTIONALITIES.md`](docs/APPLICATION-FUNCTIONALITIES.md) | Full features, roles, pages, KPIs |
| [`docs/WHY-KAFKA.md`](docs/WHY-KAFKA.md) | Kafka justification |
| [`docs/BENCHMARK-RESULTS.md`](docs/BENCHMARK-RESULTS.md) | Measured benchmark |
| [`challenge-publisher/README.md`](challenge-publisher/README.md) | Publisher modes |
| [`frontend/README.md`](frontend/README.md) | Frontend (Node 25+, Vite) |

---

## Other product features

- Soft-delete (challenges, users, questions, notifications, feedbacks) with feedbacks kept for admins
- Integration / audit logs (`CONFIG`, sync, auth, feedback, …)
- FR / EN i18n + light / dark theme
- Pagination on main lists
- Password reset requests (admin app)
