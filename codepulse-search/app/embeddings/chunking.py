from app.core.constants import DEFAULT_CHUNK_OVERLAP, DEFAULT_CHUNK_SIZE


def chunk_text(
    text: str,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    overlap: int = DEFAULT_CHUNK_OVERLAP,
) -> list[str]:
    """Fixed-size chunking with overlap (default strategy)."""
    # TODO: split on characters/tokens with `overlap`, drop empty chunks
    _ = (text, chunk_size, overlap)
    return []
