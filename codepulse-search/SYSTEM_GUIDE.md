# CodePulse Search — Complete System Guide

A from-scratch explanation of the semantic search service: what every piece is, why it exists, how data flows, how to run it, how to test it in the app and in Postman.

Audience: someone who has never built a search/AI feature before, and needs to rebuild or explain this system.

---

## Table of contents

1. [Big picture in plain language](#1-big-picture-in-plain-language)
2. [Glossary (read this first)](#2-glossary-read-this-first)
3. [The three products](#3-the-three-products)
4. [Components and what each one does](#4-components-and-what-each-one-does)
5. [Database tables (who owns what)](#5-database-tables-who-owns-what)
6. [How indexing works (the “learning” pipeline)](#6-how-indexing-works-the-learning-pipeline)
7. [How continuous learning works](#7-how-continuous-learning-works)
8. [How hybrid search works (step by step)](#8-how-hybrid-search-works-step-by-step)
9. [How KPI answers work](#9-how-kpi-answers-work)
10. [How the assistant (RAG) works](#10-how-the-assistant-rag-works)
11. [Company documents (Capgemini knowledge)](#11-company-documents-capgemini-knowledge)
12. [Authentication (same JWT as Spring)](#12-authentication-same-jwt-as-spring)
13. [Build and run from scratch](#13-build-and-run-from-scratch)
14. [Test from the web app](#14-test-from-the-web-app)
15. [Postman: prepare and run requests](#15-postman-prepare-and-run-requests)
16. [Folder map (where code lives)](#16-folder-map-where-code-lives)
17. [Troubleshooting](#17-troubleshooting)
18. [Manager talk track](#18-manager-talk-track)

---

## 1. Big picture in plain language

CodePulse already has a **Spring Boot** backend and a **React** frontend. Users log in, manage challenges, submit feedbacks, etc. That data lives in **PostgreSQL**.

We added a second backend in **Python** (`codepulse-search`) that:

- reads the same Postgres database,
- turns text into **vectors** (lists of numbers that represent meaning),
- lets admins **search by meaning**, ask **KPI questions**, and use an **assistant** that answers only from retrieved text,
- can also store **manual company documents** (e.g. Capgemini notes).

```
Browser (React :4200)
    │
    ├─ normal CRUD ──────────► Spring Boot (:8080) ──► Postgres
    │
    └─ /search /kpi /assistant /knowledge
         (Vite proxy) ───────► codepulse-search (:8090) ──► same Postgres
                                                         (+ pgvector)
```

Important ideas:

- Spring remains the **source of truth** for business data.
- The search service **never invents KPI numbers**; it runs SQL.
- “Learning” means **indexing content**, not training a neural network from scratch.
- The index can start **empty** and grow as data arrives.

---

## 2. Glossary (read this first)

| Term | Simple meaning |
|------|----------------|
| **API** | A URL you call with HTTP (GET/POST…) to ask a service to do something. |
| **JWT** | A signed login token. After login, you send `Authorization: Bearer <token>` on protected calls. |
| **Embedding** | Turning a sentence into a fixed-length list of numbers (here: 384 numbers) so “similar meaning ≈ similar numbers”. |
| **Vector** | That list of numbers. |
| **Chunk** | A short piece of a long text. We embed chunks, not entire books at once. |
| **pgvector** | A PostgreSQL extension that stores vectors and finds nearest neighbours. |
| **Semantic search** | Find documents by meaning (“trop difficile”) not only exact keywords. |
| **Keyword / full-text search** | Classic word matching inside Postgres (`tsvector`). |
| **Hybrid search** | Run semantic + keyword, then merge rankings. |
| **RRF (Reciprocal Rank Fusion)** | A formula to merge two ranked lists using position, not raw scores. |
| **RAG** | Retrieve useful chunks, then ask a small LLM to answer **only from those chunks**, with citations. |
| **KPI tool / resolver** | A fixed Python function that runs **real SQL** and returns a number. |
| **Content hash** | Fingerprint of a document’s text (SHA-256). If hash unchanged → skip re-embedding. |
| **Ollama** | Program that runs a small open LLM on your machine (optional for assistant wording). |

---

## 3. The three products

| Product | Endpoint | What the user gets |
|---------|----------|--------------------|
| Semantic search | `POST /search` | Ranked hits (challenge / feedback / question / document) with title + snippet |
| Conversational KPI | `POST /kpi` | Real DB metrics (average score, counts, participation…) |
| RAG assistant | `POST /assistant` | Short answer + **citations** (or top passages if Ollama is off) |

Plus:

| Product | Endpoint | Purpose |
|---------|----------|---------|
| Knowledge docs | `/knowledge/documents` | Add Capgemini / company text |
| Sync | `/ingestion/sync` | Force index update now |
| Status | `/ingestion/status` | Is the continuous learner running? |

All of the above (except public `/health`) need an **admin JWT** from Spring.

---

## 4. Components and what each one does

### 4.1 Spring Boot (`backend/`, port 8080)

- Login, users, challenges, feedbacks, questions, analytics…
- Issues RS256 JWT (`iss=codepulse-dev`, claims `roles`, `uid`, `sub`).
- Writes business rows into Postgres.

### 4.2 codepulse-search (`codepulse-search/`, port 8090)

Python FastAPI app that:

1. Validates the same JWT (public key file).
2. Indexes text into `search_chunk`.
3. Serves search / KPI / assistant / knowledge APIs.
4. Runs a **background learner** thread that periodically syncs new data.

### 4.3 React frontend (`frontend/`, port 4200)

- Vite proxies Spring paths to `:8080`.
- Vite also proxies `/search`, `/kpi`, `/assistant`, `/knowledge`, `/ingestion`, `/health` to `:8090`.
- Admin page: **`/admin/smart-search`** (Recherche / KPI / Assistant / Connaissance).

### 4.4 PostgreSQL + pgvector

One database (`codepulse`). Extension `vector` must be enabled once by a superuser.

### 4.5 MiniLM (embedding model)

Package: `sentence-transformers`, model `all-MiniLM-L6-v2`.

- ~90 MB download (first time).
- Runs on CPU (OK for a work laptop).
- Produces 384-dimensional normalized vectors.

### 4.6 Ollama (optional)

Only needed for **full prose** on `/assistant`. Without it, the assistant still retrieves passages and returns them with citations.

---

## 5. Database tables (who owns what)

### Owned by Spring (search service only reads)

| Table | Content |
|-------|---------|
| `coding_challenge` | Challenge title, description, tag, duration, soft-delete |
| `feedback` | Notes, comments, challenge snapshot fields, status |
| `question_feedback` | Form questions, choices JSON |

### Owned by codepulse-search

| Table | Content |
|-------|---------|
| `search_chunk` | Indexed pieces: `source_type`, `source_id`, `chunk_index`, `content`, `embedding` |
| `search_index_state` | Per-source content hash + last indexed time (for incremental sync) |
| `knowledge_document` | Manual company docs (title, body, category, tags, active) |

`source_type` values:

- `CHALLENGE`, `FEEDBACK`, `QUESTION`, `DOCUMENT`

---

## 6. How indexing works (the “learning” pipeline)

Pipeline code: `app/ingestion/pipeline.py` + `app/ingestion/documents.py`.

### Step A — Build a text document

For each Spring row (or knowledge doc), we build a **plain text** blob, for example:

```text
FEEDBACK | Valid Parentheses | tag:stacks
Feedback candidat sur: Valid Parentheses
Tag challenge: stacks
Keywords: feedback commentaire avis note stacks
Note globale: 4.0
Commentaire:
The problem was clear but time was short...
```

Why? Embeddings work on text. Putting type/title/tag up front improves both keyword and semantic matching.

### Step B — Chunk

`chunk_text()` splits long text into ~512-character pieces with ~64-character overlap, preferring breaks on spaces.

Why chunk?

- Models have a limited useful length.
- A long challenge description should not be one giant blob.
- Overlap avoids cutting a sentence in half and losing it.

### Step C — Embed

MiniLM turns each chunk into a 384-float vector. Vectors are L2-normalized so cosine distance works cleanly in pgvector.

Done in batches of 32 for CPU friendliness.

### Step D — Store

For each `(source_type, source_id)`:

1. Delete old chunks for that source.
2. Insert new chunks + embeddings.
3. Update `search_index_state` with the new content hash.

That makes reindex **idempotent** (safe to run again).

---

## 7. How continuous learning works

### Empty start

If `search_chunk` is empty, search returns no hits. That is intentional: the system has nothing to know yet.

### Automatic growth

When the FastAPI process starts (`app/main.py` lifespan), it starts `app/ingestion/learner.py`:

- Every `AUTO_INGEST_INTERVAL_SECONDS` (default **120**),
- Call `run_ingestion(full=False)`,
- Which:
  - rebuilds text for all live sources,
  - compares SHA-256 hashes to `search_index_state`,
  - embeds **only changed / new** sources,
  - removes index entries for deleted / inactive sources.

Config in `.env`:

```env
AUTO_INGEST_ENABLED=true
AUTO_INGEST_INTERVAL_SECONDS=120
```

### Manual sync

- UI: Smart search → **Connaissance** → Synchroniser maintenant  
- API: `POST /ingestion/sync` with `{ "full": false }`  
- Full rebuild: `{ "full": true }` or `python scripts/reindex.py`

### What “learning” is NOT

- Not fine-tuning MiniLM on Capgemini data overnight.
- Not downloading a new model per customer.
- Not inventing facts. If text is not indexed, it cannot be retrieved.

---

## 8. How hybrid search works (step by step)

Code: `app/retrieval/hybrid_search.py` and neighbours.

1. **Analyze the query** (`query_analysis.py`)  
   - Detects words like “feedback”, “challenge”, “Capgemini”.  
   - May infer a tag word (`arrays`, `trees`…).  
   - Inferred tags are a **soft** signal (not a hard SQL filter unless the user set a tag filter).

2. **Embed the query** with the same MiniLM model used at index time.

3. **Vector search** (`vector_search.py`)  
   - SQL: order chunks by cosine distance to the query vector.  
   - Drop weak neighbours below a similarity floor (~0.18).

4. **Keyword search** (`keyword_search.py`)  
   - Postgres full-text (`simple` config) + `ILIKE` fallback for exact fragments.

5. **Fuse with weighted RRF** (`fusion.py`)  
   - Score contribution ≈ `weight / (60 + rank)`.  
   - Short queries lean more on keywords; longer ones lean more on vectors.  
   - Preferred source types get a score boost.

6. **Collapse**  
   - Keep the best chunk per document (one result card per challenge/feedback/…).

7. **Titles + snippets**  
   - Load human titles from Spring / knowledge tables.  
   - Build a snippet centered on matched terms.

Role scoping example:

- `MANAGER_RH` → feedbacks, questions, documents  
- `ADMIN_CODING_CHALLENGE` → challenges, questions, documents  
- `ADMIN_CODEPULSE` → everything  

---

## 9. How KPI answers work

Hard rule: **numbers come from SQL only.**

1. User asks in natural language: *« Quelle est la moyenne des notes ? »*
2. **Rule router** (`kpi_tools/router.py`) maps keywords → tool name  
   (optional Ollama router only if rules find nothing).
3. Resolver runs SQL (`kpi_tools/resolvers.py`), e.g. `AVG(note_globale)` for `SOUMIS` feedbacks.
4. API returns `{ tool, value, explanation }`.

If the question does not match a known tool, the API says so — it does **not** guess a number.

---

## 10. How the assistant (RAG) works

1. Hybrid search retrieves top chunks for the question.  
2. Build a prompt: system rules + context blocks `[TYPE#id]` + question.  
3. If Ollama is reachable → generate a short French answer that must stay inside the context.  
4. If Ollama is down → return the retrieved passages + citations (still useful for demos).  
5. Always return `citations[]` with source type, id, snippet, score.

This prevents the model from freely inventing Capgemini or CodePulse facts that were never indexed.

---

## 11. Company documents (Capgemini knowledge)

### Why a separate table?

Spring tables are product data. Company blurbs (values, org intro, process notes) are **editorial content**. They live in `knowledge_document`, owned by the search service.

### Three ways to add them

1. **UI** — Smart search → Connaissance → paste title/body → save (indexes immediately).  
2. **Files** — drop `.md` / `.txt` into `knowledge/company/` then:

```bash
.venv/bin/python scripts/import_knowledge.py
```

3. **API** — `POST /knowledge/documents` (see Postman section).

Sample file shipped for demos:  
`knowledge/company/capgemini-overview.md`  
Replace with validated internal content before a real Capgemini demo.

After indexing, ask: *« Qui est Capgemini ? »* — top hit should be `DOCUMENT`.

---

## 12. Authentication (same JWT as Spring)

1. Client logs in: `POST http://localhost:8080/auth/login`  
2. Body: `{ "email": "...", "password": "..." }`  
3. Response: `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": ... }`  
4. Search service loads Spring’s **public key** (`JWT_PUBLIC_KEY_PATH`) and verifies RS256 + issuer.  
5. Routes require one of: `ADMIN_CODING_CHALLENGE`, `MANAGER_RH`, `ADMIN_CODEPULSE`.

Frontend stores the token in `localStorage` as `codepulse_token` and sends it on every API call. Because Vite proxies `/search` to `:8090`, the browser uses the **same token** without a second login.

---

## 13. Build and run from scratch

### Prerequisites

| Tool | Why |
|------|-----|
| PostgreSQL 17 (+ pgvector package) | Data + vectors |
| Java 21 | Spring Boot |
| Python 3.11+ | Search service |
| Node.js 20+ | Frontend |
| Ollama (optional) | Assistant prose |

### One-time Postgres

```bash
# install extension package (Linux example)
sudo apt install postgresql-17-pgvector

# enable extension once as superuser (see scripts/enable_pgvector.sql)
psql -U postgres -h localhost -d codepulse -f codepulse-search/scripts/enable_pgvector.sql
```

### Search service

```bash
cd codepulse-search
python3 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
pip install -r requirements-ml.txt # first time: large download
cp .env.example .env               # edit DB URL / JWT path if needed
python scripts/init_db.py
python scripts/import_knowledge.py # optional Capgemini sample
./run.sh                           # Windows: run.bat
```

Check:

```bash
curl http://127.0.0.1:8090/health
curl http://127.0.0.1:8090/health/ready
```

### Spring Boot

```bash
cd backend
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.6-oracle-x64   # adjust path
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open: http://localhost:4200

### Optional Ollama

```bash
ollama pull llama3.2:1b
# leave Ollama running
```

---

## 14. Test from the web app

1. Start Postgres, Spring (`:8080`), search (`:8090`), frontend (`:4200`).  
2. Log in with an **admin** account.  
3. Sidebar → **Recherche intelligente** (or dashboard card).  
4. Try tabs:

| Tab | Example |
|-----|---------|
| Recherche | `feedback stacks` / `challenge arrays` / `Qui est Capgemini ?` |
| KPI | `Quelle est la moyenne des notes ?` / `taux de participation` |
| Assistant | `Quels thèmes reviennent dans les commentaires ?` |
| Connaissance | Add a Capgemini paragraph, save, then search again |

5. On Connaissance, click **Synchroniser maintenant** after Spring data changes if you do not want to wait for the 2-minute learner.

---

## 15. Postman: prepare and run requests

### 15.1 Create an environment (recommended)

In Postman → Environments → Add `CodePulse local`:

| Variable | Initial value |
|----------|----------------|
| `spring` | `http://localhost:8080` |
| `search` | `http://localhost:8090` |
| `token` | *(leave empty — filled after login)* |

Select that environment in the top-right dropdown.

### 15.2 Request 0 — Login (get JWT)

- **Method:** `POST`  
- **URL:** `{{spring}}/auth/login`  
- **Headers:** `Content-Type: application/json`  
- **Body → raw → JSON:**

```json
{
  "email": "YOUR_ADMIN_EMAIL",
  "password": "YOUR_PASSWORD"
}
```

Send. In the response, copy `accessToken`.

**Auto-save token (optional Tests tab script):**

```javascript
const data = pm.response.json();
if (data.accessToken) {
  pm.environment.set("token", data.accessToken);
}
```

### 15.3 Common auth header for all search requests

On every request below:

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer {{token}}` |
| `Content-Type` | `application/json` |

If you get **401**, login again (token expired).  
If you get **403**, the user is not an admin role.

### 15.4 Health (no auth)

- `GET {{search}}/health`  
- `GET {{search}}/health/ready`  

Expect `"status":"ready"` and `"pgvector": true`.

### 15.5 Semantic search

- **POST** `{{search}}/search`

```json
{
  "query": "feedback stacks",
  "top_k": 10,
  "filters": {
    "source_type": null,
    "tag": null
  }
}
```

Capgemini check:

```json
{
  "query": "Qui est Capgemini ?",
  "top_k": 5,
  "filters": {
    "source_type": "DOCUMENT",
    "tag": null
  }
}
```

### 15.6 KPI

- **POST** `{{search}}/kpi`

```json
{ "question": "Quelle est la moyenne des notes ?" }
```

```json
{ "question": "taux de participation" }
```

```json
{ "question": "combien de challenges" }
```

### 15.7 Assistant

- **POST** `{{search}}/assistant`

```json
{ "question": "Quels points reviennent dans les feedbacks ?" }
```

```json
{ "question": "Présente Capgemini en deux phrases" }
```

### 15.8 Knowledge documents

**List**

- **GET** `{{search}}/knowledge/documents`

**Create**

- **POST** `{{search}}/knowledge/documents`

```json
{
  "title": "Capgemini — note interne exemple",
  "body": "Texte validé par la communication / RH…",
  "category": "company",
  "tags": "capgemini,company"
}
```

**Delete (soft)**

- **DELETE** `{{search}}/knowledge/documents/{{id}}`

### 15.9 Force sync / learner status

- **POST** `{{search}}/ingestion/sync`

```json
{ "full": false }
```

- **GET** `{{search}}/ingestion/status`

### 15.10 Postman collection tip

Create a folder `CodePulse Search` with requests in this order:

1. Login  
2. Health ready  
3. Search  
4. KPI  
5. Assistant  
6. Create knowledge doc  
7. Sync  
8. Search Capgemini  

Export the collection as JSON for your teammates.

---

## 16. Folder map (where code lives)

```
codepulse-search/
  app/
    main.py                 # FastAPI app + start learner
    config.py               # .env settings
    api/routes/             # HTTP endpoints
    ingestion/              # documents, pipeline, learner, knowledge
    embeddings/             # chunking + MiniLM provider
    retrieval/              # vector, keyword, fusion, hybrid search
    kpi_tools/              # SQL resolvers + routing
    generation/             # LLM client + RAG
    db/models.py            # SQLAlchemy maps
  knowledge/company/       # drop Capgemini .md/.txt here
  scripts/
    init_db.py
    reindex.py
    import_knowledge.py
    enable_pgvector.sql
  CONTINUOUS_LEARNING.md    # short manager note
  SYSTEM_GUIDE.md           # this file
  RUN_AND_TEST.md           # quick runbook
  TECHNICAL_DESIGN.md       # architecture decisions
```

---

## 17. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `/health/ready` pgvector false | Extension missing | Run `enable_pgvector.sql` as superuser |
| Empty search | Not indexed yet | `python scripts/reindex.py` or wait for learner |
| 401 on `/search` | Bad/missing token | Login again; check `Authorization` header |
| 403 | Role not admin | Use ADMIN_* / MANAGER_RH account |
| Frontend 502 on `/search` | Search service down | Start `./run.sh` on 8090 |
| KPI `tool: null` | Question not matched | Use keywords: moyenne, participation, challenges… |
| Assistant only snippets | Ollama not running | `ollama pull llama3.2:1b` and start Ollama |
| Capgemini not found | Doc not imported | UI Connaissance or `import_knowledge.py` |
| Sync always rewrites all | `search_index_state` empty | Normal once; next sync should show `unchanged` |

---

## 18. Manager talk track

1. **Problem:** admins need to find feedback themes, KPIs, and company context without writing SQL.  
2. **Approach:** same database, open-source embeddings, optional local LLM — no paid API required for the core demo.  
3. **Safety:** KPI numbers are SQL; RAG answers are grounded in retrieved chunks + citations.  
4. **Lifecycle:** empty index → continuous incremental sync → optional Capgemini documents.  
5. **Demo path:** login → Smart search → show search hit → show KPI JSON → show Capgemini document → show assistant citations.

---

### Suggested admin accounts (seeded)

| Email | Password | Role |
|-------|----------|------|
| `admin@codepulse.local` | `Admin1234!` | ADMIN_CODEPULSE |
| `manager.rh@codepulse.local` | `Manager1234!` | MANAGER_RH |
| `challenge.admin@codepulse.local` | `Challenge1234!` | ADMIN_CODING_CHALLENGE |

Use these in Postman login and in the web app.

---

## Related documents

| File | Use when |
|------|----------|
| `SYSTEM_GUIDE.md` | Teaching / rebuilding from scratch (this file) |
| `CONTINUOUS_LEARNING.md` | Short continuous-learning + Capgemini pitch |
| `TECHNICAL_DESIGN.md` | Design choices summary |
| `RUN_AND_TEST.md` | Quick start checklist |
| `LEARNING_PLAN.md` | Phase checklist |
