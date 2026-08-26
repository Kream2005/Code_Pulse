"""Tests for text chunking (no ML / DB required)."""

from app.embeddings.chunking import chunk_text


def test_chunk_text_empty() -> None:
    assert chunk_text("") == []
    assert chunk_text("   ") == []


def test_chunk_text_short() -> None:
    assert chunk_text("hello world") == ["hello world"]


def test_chunk_text_overlap_produces_multiple() -> None:
    text = "word " * 200
    chunks = chunk_text(text, chunk_size=100, overlap=20)
    assert len(chunks) >= 2
    assert all(len(c) <= 100 for c in chunks)
