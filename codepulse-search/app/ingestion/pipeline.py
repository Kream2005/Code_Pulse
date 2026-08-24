from sqlalchemy.orm import Session


def run_ingestion(db: Session | None = None) -> int:
    """Extract → chunk → embed → upsert into `search_chunk`."""
    # TODO: read challenges/feedbacks/questions, chunk_text, embed, upsert
    _ = db
    return 0
