# CodePulse Search — Build Plan

Open-source only. No paid cloud LLM APIs.

## Two-machine workflow

```
[ Dev machine ]  →  git push  →  GitHub  →  git pull  →  [ Windows work laptop ]
   write + test                                        run demo (i5, CPU)
```

| Role | Machine | What happens |
|------|---------|--------------|
| Develop | Linux / any | Source, scripts, unit tests |
| Ship | GitHub | Code + install scripts — **not** model binaries |
| Run / demo | Windows work laptop | `git pull`, deps once, models once, run service |

Models are downloaded on each machine (Hugging Face cache + Ollama). Same names in `.env` → same behaviour.

## Stack

| Need | Choice | Why |
|------|--------|-----|
| API | FastAPI | Light, OpenAPI docs |
| Vectors | pgvector in existing Postgres | No extra DB |
| Embeddings | `all-MiniLM-L6-v2` | Small, CPU OK |
| Keyword | Postgres full-text | Built-in |
| LLM | Ollama + `llama3.2:1b` | Local, optional until RAG |
| Auth | Spring RS256 JWT | One login |

Details and rationale: [`TECHNICAL_DESIGN.md`](TECHNICAL_DESIGN.md).  
How to run and test: [`RUN_AND_TEST.md`](RUN_AND_TEST.md).

## Concepts

1. Embedding — text → vector  
2. Chunk — split long text before embedding  
3. Semantic search — meaning similarity  
4. Keyword search — exact / lexical match  
5. Hybrid search — fuse both  
6. RAG — retrieve then answer from context + citations  
7. KPI tools — route to SQL; never invent numbers  

## Phase checklist

### Phase 0 — Foundations
- [x] Env / Ollama defaults  
- [x] Split `requirements.txt` + `requirements-ml.txt`  
- [x] Windows + Linux setup/run scripts  
- [x] `/health` + `/health/ready`  
- [x] `check_env.py`, `enable_pgvector.sql`, `init_db.py`  
- [x] JWT admin dependency  

### Phase 1 — Ingestion
- [x] Chunking with overlap  
- [x] Local MiniLM embeddings  
- [x] Documents from challenges / feedbacks / questions  
- [x] Upsert pipeline + `scripts/reindex.py`  
- [x] Reindex verified (`search_chunk` populated)  

### Phase 2 — Search API
- [x] Vector + keyword + RRF + collapse  
- [x] `POST /search` + filters + role scoping  

### Phase 3 — KPIs
- [x] SQL resolvers (average, participation, counts)  
- [x] Rule-based router (works without Ollama)  
- [x] Optional Ollama router fallback  
- [x] `POST /kpi`  

### Phase 4 — RAG assistant
- [x] Hybrid retrieve → prompt → citations  
- [x] Ollama generate when reachable; snippet fallback otherwise  
- [x] `POST /assistant`  

### Phase 5 — Polish
- [x] Technical design note  
- [x] Scheduler / continuous learner  
- [x] Admin UI wired (`/admin/smart-search`)  
- [x] Run & test guide (`RUN_AND_TEST.md`)  
- [x] Continuous learning + Capgemini knowledge docs (`CONTINUOUS_LEARNING.md`)  
- [ ] Short eval query set for the oral demo  

## Rules

1. Explain what / why  
2. Implement a small slice  
3. Test  
4. Confirm before the next step  

## Non-goals

- No paid LLM APIs  
- No large models that overload a work laptop  
- No inventing KPI numbers  
- No replacing Spring Boot  
