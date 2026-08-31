from enum import StrEnum


class Role(StrEnum):
    """Mirrors com.stage.backend.enums.Role."""

    USER = "USER"
    ADMIN_CODING_CHALLENGE = "ADMIN_CODING_CHALLENGE"
    MANAGER_RH = "MANAGER_RH"
    ADMIN_CODEPULSE = "ADMIN_CODEPULSE"


ADMIN_ROLES = (
    Role.ADMIN_CODING_CHALLENGE,
    Role.MANAGER_RH,
    Role.ADMIN_CODEPULSE,
)

JWT_ALG = "RS256"
JWT_ROLES_CLAIM = "roles"
JWT_UID_CLAIM = "uid"

SEARCH_CHUNKS_TABLE = "search_chunk"
KNOWLEDGE_DOCUMENTS_TABLE = "knowledge_document"
SEARCH_INDEX_STATE_TABLE = "search_index_state"
DEFAULT_CHUNK_SIZE = 512
DEFAULT_CHUNK_OVERLAP = 64
DEFAULT_TOP_K = 10
EMBEDDING_DIM = 384
EMBED_BATCH_SIZE = 32


class SourceType(StrEnum):
    """Indexed document types (stored in search_chunk.source_type)."""

    CHALLENGE = "CHALLENGE"
    FEEDBACK = "FEEDBACK"
    QUESTION = "QUESTION"
    DOCUMENT = "DOCUMENT"  # Manual knowledge (e.g. Capgemini company notes)
