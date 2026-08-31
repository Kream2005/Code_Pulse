"""Result summary returned by the ingestion pipeline."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class IngestionResult:
    sources_scanned: int = 0
    sources_indexed: int = 0
    sources_unchanged: int = 0
    sources_removed: int = 0
    sources_skipped_empty: int = 0
    chunks_written: int = 0
    chunks_deleted: int = 0
    by_type: dict[str, int] = field(default_factory=dict)
    mode: str = "incremental"

    def merge_type(self, source_type: str, count: int = 1) -> None:
        self.by_type[source_type] = self.by_type.get(source_type, 0) + count

    def as_dict(self) -> dict[str, object]:
        return {
            "mode": self.mode,
            "sources_scanned": self.sources_scanned,
            "sources_indexed": self.sources_indexed,
            "sources_unchanged": self.sources_unchanged,
            "sources_removed": self.sources_removed,
            "sources_skipped_empty": self.sources_skipped_empty,
            "chunks_written": self.chunks_written,
            "chunks_deleted": self.chunks_deleted,
            "by_type": self.by_type,
            "message": (
                f"Sync {self.mode} — indexed {self.sources_indexed}, "
                f"unchanged {self.sources_unchanged}, removed {self.sources_removed}, "
                f"chunks written {self.chunks_written}"
            ),
        }
