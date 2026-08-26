"""Tests for KPI keyword routing (no LLM required)."""

from app.kpi_tools.router import route_with_rules


def test_route_average_score() -> None:
    route = route_with_rules("Quelle est la moyenne des notes ?")
    assert route is not None
    assert route.tool == "get_average_score"


def test_route_average_with_tag() -> None:
    route = route_with_rules("moyenne des notes pour le tag Java")
    assert route is not None
    assert route.tool == "get_average_score"
    assert route.arguments.get("tag")


def test_route_participation() -> None:
    route = route_with_rules("Quel est le taux de participation ?")
    assert route is not None
    assert route.tool == "get_participation_rate"


def test_route_count_challenges() -> None:
    route = route_with_rules("Combien de challenges actifs ?")
    assert route is not None
    assert route.tool == "count_challenges"


def test_route_unknown() -> None:
    assert route_with_rules("Quel temps fait-il demain ?") is None
