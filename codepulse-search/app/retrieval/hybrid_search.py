from __future__ import annotations

from sqlalchemy.orm import Session

from app.core.security import CurrentUser
from app.embeddings.provider import get_embedding_provider
from app.retrieval.filters import resolve_source_types
from app.retrieval.fusion import (
    apply_source_type_boost,
    collapse_to_sources,
    reciprocal_rank_fusion,
)
from app.retrieval.keyword_search import keyword_search
from app.retrieval.query_analysis import analyze_query
from app.retrieval.titles import load_titles
from app.retrieval.vector_search import MIN_VECTOR_SCORE, vector_search
from app.schemas.search import SearchHit


def _snippet(text: str, query: str, max_len: int = 320) -> str:
    cleaned = " ".join(text.split())
    if len(cleaned) <= max_len:
        return cleaned

    terms = [t.lower() for t in query.split() if len(t) > 2]
    lower = cleaned.lower()
    best_pos = 0
    for term in terms:
        pos = lower.find(term)
        if pos >= 0:
            best_pos = max(0, pos - 40)
            break
    cut = cleaned[best_pos : best_pos + max_len]
    prefix = "…" if best_pos > 0 else ""
    suffix = "…" if best_pos + max_len < len(cleaned) else ""
    return f"{prefix}{cut.strip()}{suffix}"


def hybrid_search(
    db: Session,
    query: str,
    top_k: int,
    user: CurrentUser,
    source_type_filter: str | None = None,
    tag_filter: str | None = None,
) -> list[SearchHit]:
    """Embed query, run vector + keyword search, fuse, boost, map to API hits."""
    analysis = analyze_query(query)
    if not analysis.cleaned:
        return []

    source_types = resolve_source_types(user, source_type_filter)
    if analysis.preferred_types and source_type_filter is None:
        preferred_values = {item.value for item in analysis.preferred_types}
        narrowed = [item for item in source_types if item in preferred_values]
        # Soft preference: if preferred types are allowed, search them first pool
        # but keep all allowed types in the SQL filters — boost happens after RRF.
        _ = narrowed

    effective_tag = tag_filter  # only explicit UI/API tag filters are hard filters
    embed_text = analysis.cleaned
    if analysis.inferred_tag and not tag_filter:
        # Soft signal for embeddings / FTS — do not hard-filter (tags may differ).
        embed_text = f"{analysis.cleaned} tag:{analysis.inferred_tag}"

    query_embedding = get_embedding_provider().embed([embed_text])[0]

    fetch_k = max(top_k * 2, 12)
    # Explicit tag filter already narrows the pool — keep weak neighbours so
    # browsing by tag still returns challenges even if the free-text query
    # does not appear in challenge wording (e.g. French “difficiles”).
    vector_hits = vector_search(
        query_embedding=query_embedding,
        top_k=fetch_k,
        source_types=source_types,
        db=db,
        tag=effective_tag,
        min_score=0.0 if effective_tag else MIN_VECTOR_SCORE,
    )
    keyword_hits = keyword_search(
        query=analysis.cleaned,
        top_k=fetch_k,
        source_types=source_types,
        db=db,
        tag=effective_tag,
    )

    # Short lexical queries lean on keywords; longer ones lean on vectors.
    if analysis.is_short:
        weights = [0.85, 1.25]
    else:
        weights = [1.25, 0.9]

    fused = reciprocal_rank_fusion(
        [vector_hits, keyword_hits],
        weights=weights,
    )
    if analysis.preferred_types:
        fused = apply_source_type_boost(
            fused,
            tuple(item.value for item in analysis.preferred_types),
        )

    collapsed = collapse_to_sources(fused, top_k=top_k)
    titles = load_titles(db, [hit.source_key for hit in collapsed])

    results: list[SearchHit] = []
    for hit in collapsed:
        source_key = hit.source_key
        results.append(
            SearchHit(
                source_type=hit.source_type,
                source_id=hit.source_id,
                title=titles.get(source_key),
                snippet=_snippet(hit.content, analysis.cleaned),
                score=round(hit.score, 6),
            )
        )
    return results
