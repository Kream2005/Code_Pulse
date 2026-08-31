# codepulse-search

Standalone **Python** service next to the CodePulse Spring Boot backend.

- **Admin semantic search** over challenges, feedbacks, and questions  
- **Conversational KPIs** (SQL truth — never invented numbers)  
- **RAG assistant** with citations  

Same **PostgreSQL** + same **RS256 JWTs** as Spring. **No Docker.** Open-source models only (MiniLM + local **Ollama**).

Designed to **run on a Windows work laptop** (i5-class, CPU). Develop anywhere → push GitHub → pull & run on Windows.

Learning path: [`LEARNING_PLAN.md`](LEARNING_PLAN.md).  
**Full from-scratch guide (recommended):** [`SYSTEM_GUIDE.md`](SYSTEM_GUIDE.md).  
Architecture & decisions: [`TECHNICAL_DESIGN.md`](TECHNICAL_DESIGN.md).  
Run & test: [`RUN_AND_TEST.md`](RUN_AND_TEST.md).  
Continuous learning & company docs: [`CONTINUOUS_LEARNING.md`](CONTINUOUS_LEARNING.md).

## Phase 0 — first run

### Windows (work laptop)

1. Install [Python 3.11+](https://www.python.org/downloads/) (tick “Add to PATH”).
2. Postgres must already work for CodePulse (`codepulse` DB).
3. Install [pgvector](https://github.com/pgvector/pgvector) for your Postgres major version.
4. In PowerShell:

```powershell
cd codepulse-search
powershell -ExecutionPolicy Bypass -File .\setup.ps1
# Once as postgres admin:
#   psql -U postgres -d codepulse -f scripts\enable_pgvector.sql
.\.venv\Scripts\python.exe scripts\init_db.py
.\run.bat
```

### Linux (dev)

```bash
cd codepulse-search
chmod +x setup.sh run.sh
./setup.sh
# Once: sudo -u postgres psql -d codepulse -f scripts/enable_pgvector.sql
.venv/bin/python scripts/init_db.py
./run.sh
```

- API: http://localhost:8090  
- Liveness: http://localhost:8090/health  
- Readiness: http://localhost:8090/health/ready  
- Docs: http://localhost:8090/docs  

### Optional (Phase 3/4) — Ollama

1. Install from https://ollama.com (Windows installer exists).  
2. `ollama pull llama3.2:1b`  
3. Keep Ollama running; `.env` already points at `http://127.0.0.1:11434/v1`.

### Phase 1 — embeddings / reindex

```text
pip install -r requirements-ml.txt
python scripts/reindex.py
```

First run downloads PyTorch + MiniLM (~1–2 GB). On an i5 laptop, reindex may take a few minutes — normal.

```powershell
# Windows
.\scripts\reindex.bat
```

### Phase 2+ — API (with Spring admin JWT)

| Endpoint | Body example |
|----------|----------------|
| `POST /search` | `{"query":"Java feedback","top_k":5}` |
| `POST /kpi` | `{"question":"Quelle est la moyenne des notes ?"}` |
| `POST /assistant` | `{"question":"Quels points reviennent dans les feedbacks ?"}` |

Header: `Authorization: Bearer <token>`

KPI answers always come from SQL. Assistant answers need Ollama for full prose; without it, top passages are returned with citations.

## JWT key

Point `JWT_PUBLIC_KEY_PATH` at Spring’s `public.key` (same machine as the backend). Keys are local/gitignored — generate them when you start the Spring demo on that PC.
