# codepulse-search

Standalone Python service next to the CodePulse Spring Boot backend. It will provide:

- **Admin semantic search** over challenges, feedbacks, and questions
- **Conversational KPIs** (natural-language questions resolved to SQL, never invented numbers)
- **RAG assistant** for in-app navigation / Q&A with citations

It shares the existing **PostgreSQL** database and validates the same **RS256 JWTs** issued by Spring (`roles`, `uid`, `iss=codepulse-dev`). No Docker — run as a process (or via systemd).

## Local run

```bash
cd codepulse-search
cp .env.example .env
# Point JWT_PUBLIC_KEY_PATH at backend/src/main/resources/public.key
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
./run.sh
```

API: `http://localhost:8090` (Spring stays on `:8080`). Docs: `http://localhost:8090/docs`.

One-off DB bits (pgvector + chunks table):

```bash
.venv/bin/python scripts/init_db.py
```

## Deploy

`deploy/codepulse-search.service` is a systemd unit that runs `run.sh`, restarts on failure, and starts on boot.
