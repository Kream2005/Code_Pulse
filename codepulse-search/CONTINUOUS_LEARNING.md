# Continuous learning & company knowledge

How CodePulse Search stays up to date, and how to add Capgemini / company documents.
Use this note when explaining the feature to managers.

---

## One-sentence pitch

> The search service starts empty. As challenges, feedbacks and questions appear in Postgres (via Spring), a background learner indexes only what changed. Admins can also paste company documents (Capgemini notes, HR blurbs, process guides). Search and the assistant then answer from **live product data + company knowledge**.

---

## What “learning” means here (important)

We do **not** train a new neural network every night.

We use a ready-made open embedding model (MiniLM). “Learning” = **indexing your content**:

1. Read rows / documents  
2. Split into chunks  
3. Convert to vectors  
4. Store in `search_chunk`  

When content changes, we re-index **only the delta** (content hash comparison).

---

## Empty start → grows by itself

```
Day 0: search_chunk empty  →  search returns nothing (honest)
       ↓
Spring creates challenges / feedbacks / questions
       ↓
Background learner (every ~2 min by default)
       ↓
Only new or modified sources are embedded
       ↓
Search / KPI / assistant become useful as data arrives
```

Deleted / soft-deleted Spring rows are removed from the index on the next sync.

Config (`.env`):

```
AUTO_INGEST_ENABLED=true
AUTO_INGEST_INTERVAL_SECONDS=120
```

Manual trigger (admin JWT): `POST /ingestion/sync`  
UI: **Recherche intelligente → Connaissance → Synchroniser maintenant**

---

## Two data streams

| Stream | Owner | Examples | How it enters the index |
|--------|--------|----------|-------------------------|
| Product data | Spring Boot tables | challenges, feedbacks, questions | Automatic learner + optional sync |
| Company knowledge | Search service table `knowledge_document` | Capgemini overview, values, FAQ interne | UI paste, or files in `knowledge/company/` |

Both become `search_chunk` rows with a `source_type`:

- `CHALLENGE` / `FEEDBACK` / `QUESTION` / `DOCUMENT`

---

## Adding Capgemini documents (for managers)

### Option A — Admin UI (demo-friendly)

1. Open **Recherche intelligente**  
2. Tab **Connaissance / Knowledge**  
3. Title + body (+ tags like `capgemini,company`)  
4. **Enregistrer et indexer**  

The document is stored and immediately indexed. Ask in Search or Assistant:  
*« Qui est Capgemini ? »* / *« Quelles sont les valeurs mises en avant ? »*

### Option B — Drop files (batch)

1. Put `.md` or `.txt` files in:

```
codepulse-search/knowledge/company/
```

2. Import:

```bash
cd codepulse-search
.venv/bin/python scripts/import_knowledge.py
```

A sample file is already there: `knowledge/company/capgemini-overview.md`  
**Replace it with validated internal content** before a real client demo.

### Option C — API

```http
POST /knowledge/documents
Authorization: Bearer <admin JWT>

{
  "title": "Capgemini — valeurs",
  "body": "...",
  "category": "company",
  "tags": "capgemini,values"
}
```

---

## Architecture diagram (talk track)

```
┌──────────────┐     writes      ┌─────────────────┐
│ Spring Boot  │ ──────────────► │ Postgres tables │
│ (CRUD métier)│                 │ challenges / FB │
└──────────────┘                 └────────┬────────┘
                                          │
┌──────────────┐   paste / files          │ poll + hash
│ Admin / RH   │ ──────────────► knowledge_document
└──────────────┘                          │
                                          ▼
                               continuous learner
                               (chunk + embed MiniLM)
                                          │
                                          ▼
                                    search_chunk
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
                 /search               /kpi                 /assistant
```

---

## Manager FAQ

**Does it invent Capgemini facts?**  
No. Answers come from indexed documents and product data. If nothing is indexed, the system says so (or returns no hits).

**Is confidential data safe?**  
Company docs live in your Postgres. They are not sent to a paid cloud LLM by default. Optional local Ollama stays on the laptop.

**What if Spring is empty?**  
Search is empty — by design. The product “learns” as real usage fills the tables.

**Do we re-index everything every time?**  
No. `search_index_state` stores a content hash per source. Unchanged rows are skipped.

---

## Ops checklist

```bash
# create tables (once)
.venv/bin/python scripts/init_db.py

# optional: import Capgemini sample / your files
.venv/bin/python scripts/import_knowledge.py

# start API (learner starts with the process)
./run.sh
```

Verify:

```bash
curl -H "Authorization: Bearer <token>" http://127.0.0.1:8090/ingestion/status
curl -H "Authorization: Bearer <token>" http://127.0.0.1:8090/knowledge/documents
```
