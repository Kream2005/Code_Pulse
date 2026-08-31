"""Function-calling schemas for KPI tools. Resolvers must run SQL — never invent numbers."""

GET_AVERAGE_SCORE = {
    "name": "get_average_score",
    "description": (
        "Average global feedback score (note_globale) for submitted feedbacks. "
        "Optional tag filter on challenge_tag."
    ),
    "parameters": {
        "type": "object",
        "properties": {
            "tag": {
                "type": "string",
                "description": "Challenge tag fragment, e.g. Java or arrays",
            },
        },
    },
}

GET_PARTICIPATION_RATE = {
    "name": "get_participation_rate",
    "description": (
        "Share of active challenges that received at least one submitted feedback."
    ),
    "parameters": {"type": "object", "properties": {}},
}

COUNT_CHALLENGES = {
    "name": "count_challenges",
    "description": "Count non-deleted coding challenges, optionally filtered by tag.",
    "parameters": {
        "type": "object",
        "properties": {
            "tag": {"type": "string", "description": "Challenge tag fragment"},
        },
    },
}

COUNT_FEEDBACKS = {
    "name": "count_feedbacks",
    "description": (
        "Count non-deleted feedbacks. Optional statut: SOUMIS or NON_SOUMIS."
    ),
    "parameters": {
        "type": "object",
        "properties": {
            "statut": {
                "type": "string",
                "enum": ["SOUMIS", "NON_SOUMIS"],
                "description": "Feedback status filter",
            },
        },
    },
}

COUNT_QUESTIONS = {
    "name": "count_questions",
    "description": "Count non-deleted feedback form questions.",
    "parameters": {"type": "object", "properties": {}},
}

KPI_TOOLS = [
    GET_AVERAGE_SCORE,
    GET_PARTICIPATION_RATE,
    COUNT_CHALLENGES,
    COUNT_FEEDBACKS,
    COUNT_QUESTIONS,
]

KPI_TOOL_NAMES = {tool["name"] for tool in KPI_TOOLS}
