# CodePulse Search — Run & Test

How to start the stack and verify search, KPI, and assistant quality on a laptop.

## 1. Prerequisites

| Service | Port | Needed for |
|---------|------|------------|
| PostgreSQL + pgvector | 5432 | data + vectors |
| Spring Boot | 8080 | login JWT + business data |
| codepulse-search | 8090 | `/search`, `/kpi`, `/assistant` |
| Frontend (Vite) | 4200 | admin UI |
| Ollama (optional) | 11434 | full RAG prose |

## 2. First-time database

```bash
# once, as Postgres superuser — enable extension
# (see scripts/enable_pgvector.sql comments if peer auth fails)

cd codepulse-search
.venv/bin/python scripts/init_db.py
.venv/bin/pip install -r requirements-ml.txt
.venv/bin/python scripts/reindex.py
```

Expect `chunks_written > 0`. Re-run `reindex.py` after bulk Spring data changes or document-builder updates.

Windows:

```powershell
.\scripts\reindex.bat
```

## 3. Start services

Terminal A — Spring (from `backend/` as usual).

Terminal B — search:

```bash
cd codepulse-search
./run.sh
# Windows: run.bat
```

Check:

```bash
curl http://127.0.0.1:8090/health/ready
```

`database` + `pgvector` should be true.

Terminal C — frontend:

```bash
cd frontend
npm install   # first time
npm run dev
```

Open http://localhost:4200 — Vite proxies `/search`, `/kpi`, `/assistant` to `:8090` and the rest to Spring `:8080`.

Optional — Ollama for assistant wording:

```bash
ollama pull llama3.2:1b
# keep Ollama running
```

## 4. UI test path

1. Log in as an admin (`ADMIN_CODEPULSE`, `MANAGER_RH`, or `ADMIN_CODING_CHALLENGE`).
2. Open **Recherche intelligente** / **Smart search** (`/admin/smart-search`).
3. Tabs:
   - **Recherche** — semantic + keyword hybrid results  
   - **KPI** — SQL metrics (works without Ollama)  
   - **Assistant** — RAG; with Ollama = prose, without = top passages + citations  

### Suggested queries

| Tab | Query |
|-----|--------|
| Recherche | `feedbacks trop difficiles` |
| Recherche | `challenge arrays` or `trees` |
| Recherche | `feedback stacks` + optional tag filter `stacks` |
| KPI | `Quelle est la moyenne des notes ?` |
| KPI | `taux de participation` |
| KPI | `combien de challenges` |
| Assistant | `Quels thèmes reviennent dans les commentaires ?` |

Note: demo challenge tags are things like `arrays`, `trees`, `stacks`, `graphs`, `dp` — not language names.

## 5. API / Postman

Get a JWT from Spring login (`POST /auth/login`), then:

```http
POST http://localhost:8090/search
Authorization: Bearer <token>
Content-Type: application/json

{
  "query": "feedback Java",
  "top_k": 10,
  "filters": { "source_type": "FEEDBACK", "tag": "Java" }
}
```

```http
POST http://localhost:8090/kpi
Authorization: Bearer <token>

{ "question": "Quelle est la moyenne des notes ?" }
```

```http
POST http://localhost:8090/assistant
Authorization: Bearer <token>

{ "question": "Résume les points négatifs des feedbacks" }
```

Via the frontend proxy (same browser session cookie not used — still send Bearer if calling from Postman against `:4200`):

`http://localhost:4200/search` → proxied to `:8090`.

## 6. Quality checklist

Search quality improvements in this build:

- Hybrid retrieval (vectors + full-text + ILIKE)  
- Weighted RRF (short queries lean lexical; long queries lean semantic)  
- Cosine similarity floor (~0.22) to drop weak neighbours  
- Query analysis: boosts FEEDBACK / CHALLENGE / QUESTION when named; infers common tags  
- Document builders put `TYPE | title | tag:` in the first lines  
- HNSW index on embeddings + GIN on full-text  
- Snippets centered on matched terms  

If results look stale after code changes to `documents.py`, **reindex**.

## 7. Automated tests (search service)

```bash
cd codepulse-search
.venv/bin/python -m pytest tests/ -q
```

## 8. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Empty search results | Run `reindex.py`; check `SELECT COUNT(*) FROM search_chunk` |
| 401/403 on `/search` | Use a fresh admin JWT from Spring |
| Frontend 502 on `/search` | Start codepulse-search on 8090 |
| KPI tool null | Rephrase: moyenne, participation, challenges, feedbacks |
| Assistant only shows snippets | Start Ollama + `ollama pull llama3.2:1b` |
| pgvector missing | Superuser `CREATE EXTENSION vector` then `init_db.py` |
