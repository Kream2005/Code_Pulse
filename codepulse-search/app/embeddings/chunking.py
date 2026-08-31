from app.core.constants import DEFAULT_CHUNK_OVERLAP, DEFAULT_CHUNK_SIZE


def chunk_text(
    text: str,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    overlap: int = DEFAULT_CHUNK_OVERLAP,
) -> list[str]:
    """Split text into overlapping chunks (character-based, word-aware breaks).

    Why character-based (not tokens)?
    - No extra tokenizer dependency
    - Good enough for demo-scale CodePulse content on a CPU laptop
    - MiniLM accepts ~512 tokens; 512 chars ≈ safe upper bound for short fields
    """
    normalized = (text or "").strip()
    if not normalized:
        return []
    if len(normalized) <= chunk_size:
        return [normalized]

    if overlap >= chunk_size:
        overlap = max(0, chunk_size // 4)

    chunks: list[str] = []
    start = 0
    length = len(normalized)

    while start < length:
        end = min(start + chunk_size, length)
        if end < length:
            window = normalized[start:end]
            break_at = window.rfind(" ")
            if break_at >= chunk_size // 2:
                end = start + break_at

        piece = normalized[start:end].strip()
        if piece:
            chunks.append(piece)

        if end >= length:
            break
        start = max(end - overlap, start + 1)

    return chunks
