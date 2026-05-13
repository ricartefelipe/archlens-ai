import re
from abc import ABC, abstractmethod

from app.chunking.chunker import TextChunker


class ChunkingStrategy(ABC):
    @abstractmethod
    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        ...


class GenericChunker(ChunkingStrategy):
    def __init__(self) -> None:
        self._chunker = TextChunker()

    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        return self._chunker.chunk_file(file_path, content)


class JavaChunker(ChunkingStrategy):
    _CLASS_OR_METHOD = re.compile(
        r"^(?=\s*(?:public|private|protected|static|final|abstract|synchronized|native|default)*"
        r"\s*(?:class|interface|enum|record|void|int|long|float|double|boolean|char|byte|short|"
        r"String|List|Map|Set|Optional|[\w<>\[\]]+)\s+\w+)",
        re.MULTILINE,
    )

    def __init__(self) -> None:
        self._fallback = TextChunker()

    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        boundaries = [m.start() for m in self._CLASS_OR_METHOD.finditer(content)]
        if len(boundaries) < 2:
            return self._fallback.chunk_file(file_path, content)

        sections: list[str] = []
        for i, start in enumerate(boundaries):
            end = boundaries[i + 1] if i + 1 < len(boundaries) else len(content)
            sections.append(content[start:end])

        preamble = content[: boundaries[0]]
        if preamble.strip():
            sections.insert(0, preamble)

        chunks: list[dict] = []
        for section in sections:
            if not section.strip():
                continue
            chunks.append(
                {
                    "content": section,
                    "chunk_index": len(chunks),
                    "metadata": {
                        "file_path": file_path,
                        "start_char": content.index(section),
                        "end_char": content.index(section) + len(section),
                    },
                }
            )
        return chunks


class YamlChunker(ChunkingStrategy):
    _TOP_LEVEL_KEY = re.compile(r"^(?!\s)(\S.*)", re.MULTILINE)

    def __init__(self) -> None:
        self._fallback = TextChunker()

    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        boundaries = [m.start() for m in self._TOP_LEVEL_KEY.finditer(content)]
        if len(boundaries) < 2:
            return self._fallback.chunk_file(file_path, content)

        chunks: list[dict] = []
        for i, start in enumerate(boundaries):
            end = boundaries[i + 1] if i + 1 < len(boundaries) else len(content)
            section = content[start:end]
            if not section.strip():
                continue
            chunks.append(
                {
                    "content": section,
                    "chunk_index": len(chunks),
                    "metadata": {
                        "file_path": file_path,
                        "start_char": start,
                        "end_char": end,
                    },
                }
            )
        return chunks


class SqlChunker(ChunkingStrategy):
    _STATEMENT = re.compile(
        r"^\s*(?:CREATE|ALTER|DROP|INSERT|UPDATE|DELETE|SELECT|GRANT|REVOKE|COMMENT|BEGIN|COMMIT)\b",
        re.MULTILINE | re.IGNORECASE,
    )

    def __init__(self) -> None:
        self._fallback = TextChunker()

    def chunk_file(self, file_path: str, content: str) -> list[dict]:
        boundaries = [m.start() for m in self._STATEMENT.finditer(content)]
        if len(boundaries) < 2:
            return self._fallback.chunk_file(file_path, content)

        chunks: list[dict] = []
        for i, start in enumerate(boundaries):
            end = boundaries[i + 1] if i + 1 < len(boundaries) else len(content)
            section = content[start:end]
            if not section.strip():
                continue
            chunks.append(
                {
                    "content": section,
                    "chunk_index": len(chunks),
                    "metadata": {
                        "file_path": file_path,
                        "start_char": start,
                        "end_char": end,
                    },
                }
            )
        return chunks


_EXTENSION_MAP: dict[str, type[ChunkingStrategy]] = {
    ".java": JavaChunker,
    ".kt": JavaChunker,
    ".scala": JavaChunker,
    ".yml": YamlChunker,
    ".yaml": YamlChunker,
    ".sql": SqlChunker,
}


class ChunkerFactory:
    @staticmethod
    def get_chunker(file_path: str) -> ChunkingStrategy:
        for ext, cls in _EXTENSION_MAP.items():
            if file_path.endswith(ext):
                return cls()
        return GenericChunker()
