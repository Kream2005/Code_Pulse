"""Placeholder for authenticated HTTP assistant tests (needs Ollama + JWT)."""


def test_rag_module_imports() -> None:
    from app.generation.rag_pipeline import run_rag

    assert callable(run_rag)
