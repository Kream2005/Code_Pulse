# CodePulse Search — Technical Design

Companion service for the CodePulse Spring Boot backend.  
Stack: FastAPI, PostgreSQL + pgvector, sentence-transformers (MiniLM), Ollama (optional local LLM).

This note explains the architecture, the choices made for a CPU-only Windows laptop demo, and how each build phase fits together.

---

## 1. Goals

Three admin-facing capabilities on the same CodePulse data:

1. **Semantic search** over challenges, feedbacks, and feedback-form questions  
2. **Conversational KPIs** where every number comes from SQL (never invented)  
3. **RAG assistant** that answers from retrieved chunks and returns citations  

Constraints that shaped the design:

- Open-source models only (no paid APIs)  
- Same Postgres instance and RS256 JWT as Spring Boot  
- Runnable on a Windows work laptop (Intel i5 class, no dedicated GPU)  
- No Docker requirement  
- Model weights are **not** committed to git (downloaded per machine)

---

## 2. High-level architecture

```
┌────────────────────┐     JWT (RS256)      ┌──────────────────────────┐
│  Spring Boot :8080 │◄────────────────────►│  codepulse-search :8090  │
│  challenges / FB / │                      │  search / kpi / assistant│
│  questions CRUD    │                      └────────────┬─────────────┘
└─────────┬──────────┘                                   │
          │                                              │
          └─────────────── same PostgreSQL ──────────────┘
                           + pgvector extension
                           table search_chunk (owned by search service)
```

| Component | Role |
|-----------|------|
| Spring Boot | Source of truth for business data and auth |
| `search_chunk` | Chunks + embeddings owned by this service |
| MiniLM (`all-MiniLM-L6-v2`) | Text → 384-dim vectors on CPU |
| Ollama (`llama3.2:1b`) | Optional: KPI tool routing fallback + RAG wording |
| FastAPI | HTTP API on port 8090 |

---

## 3. Concepts (short glossary)

| Term | Meaning here |
|------|----------------|
| Embedding | List of numbers representing text meaning |
| Chunk | Slice of a document (with overlap) before embedding |
| Semantic search | Match by meaning via vector distance |
| Keyword / full-text | Match by words (`tsvector` / `tsquery`) |
| Hybrid search | Fuse vector + keyword rankings |
| RRF | Reciprocal Rank Fusion: combine ranks without mixing incompatible scores |
| RAG | Retrieve chunks, then ask the LLM to answer only from that context |
| KPI tool | Named SQL resolver; the LLM (or rules) only *selects* the tool |

---

## 4. Runtime target and dependency split

**Primary demo machine:** Windows 10/11, i5 11th gen, CPU only.

| File | Contents | When |
|------|----------|------|
| `requirements.txt` | FastAPI, SQLAlchemy, pgvector, JWT, httpx | Always |
| `requirements-ml.txt` | torch, sentence-transformers | Before first reindex / search |

Why split: health checks and KPI SQL can run without downloading ~1–2 GB of ML stacks. Embeddings load lazily on first use.

Scripts:

- Windows: `setup.ps1`, `run.bat`, `scripts/reindex.bat`  
- Linux: `setup.sh`, `run.sh`  

---

## 5. Phase 0 — Foundations

### What was delivered

- `.env` / `.env.example` with Ollama defaults (not a cloud LLM vendor)  
- Config resolution for JWT public key relative to the project root  
- `GET /health` (liveness) and `GET /health/ready` (DB, pgvector, JWT key, Ollama reachability)  
- `scripts/check_env.py`, `scripts/enable_pgvector.sql`, `scripts/init_db.py`  
- Auth dependency: decode Spring RS256 JWT, enforce admin roles  

### Why these choices

| Choice | Justification |
|--------|----------------|
| Same Postgres | No second database; KPI SQL and vectors share one source of truth |
| pgvector | Vectors live next to relational data; no Elastic/OpenSearch ops cost |
| Same JWT | One login for admins; roles already defined in Spring |
| Ollama in config early | Ready for Phase 3/4 without redesigning settings |
| Split ML requirements | Faster first boot on a work laptop |

### Operational note (pgvector)

`CREATE EXTENSION vector` needs a Postgres superuser once. App user `codepulse` is intentionally not a superuser. After the extension exists, `init_db.py` creates `search_chunk` and indexes as the app user.

---

## 6. Phase 1 — Ingestion

### Pipeline

```
Spring tables (non-deleted)
        → document builders (plain text)
        → chunk_text (512 chars, 64 overlap, word-aware)
        → MiniLM embed (batches of 32, L2-normalized)
        → delete previous chunks for (source_type, source_id)
        → insert into search_chunk
```

### Document builders (`app/ingestion/documents.py`)

| Source | Indexed fields (summary) |
|--------|---------------------------|
| Challenge | title, tag, duration, description |
| Feedback | challenge context, note, status, comment |
| Question | label, type, required flag, choice options (JSON) |

### Why character chunking (not tokenizers)

- No extra tokenizer dependency  
- Demo content is short enough that 512 characters stays within MiniLM’s practical window  
- Overlap reduces loss at boundaries  

### Why MiniLM

- ~90 MB model, 384 dimensions  
- Proven CPU performance for semantic search demos  
- Same model used at query time (must match indexed vectors)  

### Idempotent reindex

Per-source delete-then-insert avoids duplicate chunks when re-running `scripts/reindex.py`.

Verified on the development database after reindex: thousands of rows in `search_chunk` covering challenges, feedbacks, and questions.

---

## 7. Phase 2 — Hybrid search API

### Endpoint

`POST /search` (admin JWT required)

Request: query, optional `filters.source_type` / `filters.tag`, `top_k`.  
Response: ranked hits with `source_type`, `source_id`, `title`, `snippet`, `score`.

### Flow

1. Embed the query with the same MiniLM provider  
2. **Vector search** — `ORDER BY embedding <=> query` (cosine distance via pgvector)  
3. **Keyword search** — `to_tsvector('simple', content) @@ plainto_tsquery(...)`  
4. **RRF** — merge ranks: `1 / (60 + rank)` summed across lists  
5. **Collapse** — keep the best chunk per `(source_type, source_id)`  
6. Load display titles from Spring tables  

### Why hybrid + RRF

- Vectors catch paraphrases; keywords catch exact tags / rare tokens  
- RRF avoids ad-hoc score normalization between cosine similarity and `ts_rank`  
- Items present in both lists naturally rise  

### Role scoping

| Role | Allowed sources |
|------|-----------------|
| `ADMIN_CODEPULSE` | Challenge, Feedback, Question |
| `ADMIN_CODING_CHALLENGE` | Challenge, Question |
| `MANAGER_RH` | Feedback, Question |

Request filters cannot widen beyond the caller’s roles.

### Full-text config

`simple` (no French stemming) was chosen for predictable behaviour on mixed FR/EN content and tag-like tokens. Can switch to `french` later if stemming proves useful.

---

## 8. Phase 3 — Conversational KPIs

### Hard rule

**Numbers always come from SQL resolvers.** Routing may use keywords or a local LLM; neither invents a metric.

### Resolvers (`app/kpi_tools/resolvers.py`)

| Tool | SQL meaning |
|------|-------------|
| `get_average_score` | AVG(`note_globale`) for `SOUMIS` feedbacks (optional tag) |
| `get_participation_rate` | Challenges with ≥1 submitted feedback / active challenges |
| `count_challenges` | Non-deleted challenges (optional tag) |
| `count_feedbacks` | Non-deleted feedbacks (optional statut) |
| `count_questions` | Non-deleted feedback questions |

### Routing strategy

1. **Rules first** (French/English keywords) — works offline without Ollama  
2. **Optional Ollama** — only if rules find nothing and Ollama is reachable  
3. Otherwise return a clear “unknown tool” message  

Endpoint: `POST /kpi`

Example (rules path): question *« Quelle est la moyenne des notes ? »* → `get_average_score` → real `AVG` from Postgres.

---

## 9. Phase 4 — RAG assistant

### Endpoint

`POST /assistant`

### Flow

1. Hybrid search with the caller’s role filter  
2. Build a grounded prompt (system instruction + context blocks `[TYPE#id]`)  
3. If Ollama is down: return the top snippets with a clear message (no fake prose)  
4. If Ollama is up: generate a short French answer constrained to the context  
5. Always return **citations** (source type, id, snippet, score)

### Why a tiny local model

Laptop CPU cannot host large models for a stage demo. `llama3.2:1b` (or similar 1–3B) is enough to phrase an answer; retrieval quality matters more than model size.

### Install (when generating answers)

1. Install Ollama for Windows/Linux  
2. `ollama pull llama3.2:1b`  
3. Keep the daemon running; `.env` already points at `http://127.0.0.1:11434/v1`  

---

## 10. Phase 5 — Operations polish

| Item | Approach |
|------|----------|
| Reindex cadence | `scripts/reindex.py` manually, or cron / `app/ingestion/scheduler.py` loop |
| Health for demos | Prefer `/health/ready` before showing Postman flows |
| Eval | Small hand-built query set (search relevance + KPI known answers) |
| Frontend | Optional later; Postman/OpenAPI (`/docs`) is enough for backend demo |

---

## 11. Security summary

- JWT validation mirrors Spring (RS256, issuer, `roles`, `uid`)  
- Search / KPI / assistant routes require admin roles  
- Soft-deleted Spring rows are excluded at ingestion and in KPI SQL  
- Secrets stay in local `.env` / Spring `application.properties` (gitignored)  

---

## 12. Main HTTP surface

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/health` | public | Process up |
| GET | `/health/ready` | public | Dependencies |
| POST | `/search` | admin JWT | Hybrid retrieval |
| POST | `/kpi` | admin JWT | SQL-backed metrics |
| POST | `/assistant` | admin JWT | RAG + citations |

OpenAPI UI: `http://localhost:8090/docs`

---

## 13. Suggested demo checklist

1. Spring Boot up on `:8080`, search service on `:8090`  
2. `GET /health/ready` → database + pgvector true  
3. Admin login → copy JWT  
4. `POST /search` with a realistic query  
5. `POST /kpi` with « moyenne des notes » and « taux de participation »  
6. (Optional) Start Ollama → `POST /assistant` and show citations  

Reindex after bulk Spring data changes:

```bash
# Linux
.venv/bin/python scripts/reindex.py

# Windows
.\scripts\reindex.bat
```

---

## 14. Explicit non-goals

- Replacing Spring Boot  
- Training a custom neural network from scratch  
- Inventing KPI numbers when SQL returns null/empty  
- Shipping multi‑GB model weights in the git repository  
- Requiring GPU or Docker for the stage demo  
