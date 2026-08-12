from sqlalchemy.orm import Session


def get_average_score(db: Session, tag: str | None = None) -> float | None:
    """Average `feedback.note_globale` (submitted, not deleted)."""
    # TODO: real SQL against feedback / coding_challenge — never invent a number
    _ = (db, tag)
    return None


def get_participation_rate(db: Session) -> float | None:
    """Challenges with submitted feedback / active challenges."""
    # TODO: real SQL against feedback / coding_challenge — never invent a number
    _ = db
    return None
