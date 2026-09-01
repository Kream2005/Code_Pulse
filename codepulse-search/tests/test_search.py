"""Placeholder for authenticated HTTP search tests (needs live JWT + DB)."""


def test_search_module_imports() -> None:
    from app.retrieval.hybrid_search import hybrid_search

    assert callable(hybrid_search)
