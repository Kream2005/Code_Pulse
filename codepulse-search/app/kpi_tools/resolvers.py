"""SQL-backed KPI resolvers. Values always come from Postgres — never invented."""

from __future__ import annotations

from typing import Any

from sqlalchemy import text
from sqlalchemy.orm import Session


def get_average_score(db: Session, tag: str | None = None) -> dict[str, Any]:
    """Average `feedback.note_globale` for submitted, non-deleted feedbacks."""
    if tag:
        row = db.execute(
            text(
                """
                SELECT
                    AVG(f.note_globale) AS avg_note,
                    COUNT(*)::int AS sample_size
                FROM feedback f
                WHERE f.supprime = FALSE
                  AND f.statut_feedback = 'SOUMIS'
                  AND f.note_globale IS NOT NULL
                  AND f.challenge_tag ILIKE :tag
                """
            ),
            {"tag": f"%{tag.strip()}%"},
        ).mappings().one()
    else:
        row = db.execute(
            text(
                """
                SELECT
                    AVG(f.note_globale) AS avg_note,
                    COUNT(*)::int AS sample_size
                FROM feedback f
                WHERE f.supprime = FALSE
                  AND f.statut_feedback = 'SOUMIS'
                  AND f.note_globale IS NOT NULL
                """
            )
        ).mappings().one()

    sample_size = int(row["sample_size"] or 0)
    avg_note = row["avg_note"]
    return {
        "average": round(float(avg_note), 4) if avg_note is not None else None,
        "sample_size": sample_size,
        "tag": tag,
        "statut": "SOUMIS",
    }


def get_participation_rate(db: Session) -> dict[str, Any]:
    """Share of active challenges that have at least one submitted feedback."""
    row = db.execute(
        text(
            """
            SELECT
                (
                    SELECT COUNT(*)::int
                    FROM coding_challenge c
                    WHERE c.supprime = FALSE
                ) AS active_challenges,
                (
                    SELECT COUNT(DISTINCT f.coding_challenge_id)::int
                    FROM feedback f
                    WHERE f.supprime = FALSE
                      AND f.statut_feedback = 'SOUMIS'
                      AND f.coding_challenge_id IS NOT NULL
                ) AS challenges_with_feedback
            """
        )
    ).mappings().one()

    active = int(row["active_challenges"] or 0)
    with_feedback = int(row["challenges_with_feedback"] or 0)
    rate = (with_feedback / active) if active > 0 else None
    return {
        "active_challenges": active,
        "challenges_with_feedback": with_feedback,
        "rate": round(rate, 6) if rate is not None else None,
    }


def count_challenges(db: Session, tag: str | None = None) -> dict[str, Any]:
    if tag:
        row = db.execute(
            text(
                """
                SELECT COUNT(*)::int AS total
                FROM coding_challenge c
                WHERE c.supprime = FALSE
                  AND c.tag ILIKE :tag
                """
            ),
            {"tag": f"%{tag.strip()}%"},
        ).mappings().one()
    else:
        row = db.execute(
            text(
                """
                SELECT COUNT(*)::int AS total
                FROM coding_challenge c
                WHERE c.supprime = FALSE
                """
            )
        ).mappings().one()
    return {"total": int(row["total"] or 0), "tag": tag}


def count_feedbacks(db: Session, statut: str | None = None) -> dict[str, Any]:
    if statut:
        row = db.execute(
            text(
                """
                SELECT COUNT(*)::int AS total
                FROM feedback f
                WHERE f.supprime = FALSE
                  AND f.statut_feedback = :statut
                """
            ),
            {"statut": statut.strip().upper()},
        ).mappings().one()
    else:
        row = db.execute(
            text(
                """
                SELECT COUNT(*)::int AS total
                FROM feedback f
                WHERE f.supprime = FALSE
                """
            )
        ).mappings().one()
    return {"total": int(row["total"] or 0), "statut": statut}


def count_questions(db: Session) -> dict[str, Any]:
    row = db.execute(
        text(
            """
            SELECT COUNT(*)::int AS total
            FROM question_feedback q
            WHERE q.supprime = FALSE
            """
        )
    ).mappings().one()
    return {"total": int(row["total"] or 0)}


RESOLVERS = {
    "get_average_score": get_average_score,
    "get_participation_rate": get_participation_rate,
    "count_challenges": count_challenges,
    "count_feedbacks": count_feedbacks,
    "count_questions": count_questions,
}


def run_resolver(name: str, db: Session, **kwargs: Any) -> dict[str, Any]:
    fn = RESOLVERS.get(name)
    if fn is None:
        raise ValueError(f"Unknown KPI tool: {name}")
    return fn(db, **kwargs)
