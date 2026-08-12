"""Function-calling schemas for KPI tools. Resolvers must run SQL — never invent numbers."""

GET_AVERAGE_SCORE = {
    "name": "get_average_score",
    "description": "Average global feedback score, optionally filtered by tag.",
    "parameters": {
        "type": "object",
        "properties": {
            "tag": {"type": "string", "description": "Challenge tag, e.g. arrays"},
        },
    },
}

GET_PARTICIPATION_RATE = {
    "name": "get_participation_rate",
    "description": "Share of challenges that received submitted feedback.",
    "parameters": {
        "type": "object",
        "properties": {},
    },
}

KPI_TOOLS = [GET_AVERAGE_SCORE, GET_PARTICIPATION_RATE]
