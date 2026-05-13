import hashlib
import random
import struct
from abc import ABC, abstractmethod

import httpx

from app.config import settings


class EmbeddingGateway(ABC):
    @abstractmethod
    async def generate(self, texts: list[str]) -> list[list[float]]:
        ...


class LocalEmbeddingGateway(EmbeddingGateway):
    def __init__(self, dimension: int = 1536) -> None:
        self._dimension = dimension

    async def generate(self, texts: list[str]) -> list[list[float]]:
        return [self._deterministic_embedding(text) for text in texts]

    def _deterministic_embedding(self, text: str) -> list[float]:
        digest = hashlib.sha256(text.encode("utf-8")).digest()
        seed = struct.unpack("<I", digest[:4])[0]
        rng = random.Random(seed)
        raw = [rng.gauss(0, 1) for _ in range(self._dimension)]
        norm = sum(x * x for x in raw) ** 0.5
        return [x / norm for x in raw]


class OpenAIEmbeddingGateway(EmbeddingGateway):
    BATCH_SIZE = 100

    def __init__(self) -> None:
        self._api_key = settings.openai_api_key
        self._model = settings.embedding_model

    async def generate(self, texts: list[str]) -> list[list[float]]:
        all_embeddings: list[list[float]] = []
        async with httpx.AsyncClient(timeout=60.0) as client:
            for i in range(0, len(texts), self.BATCH_SIZE):
                batch = texts[i : i + self.BATCH_SIZE]
                response = await client.post(
                    "https://api.openai.com/v1/embeddings",
                    headers={"Authorization": f"Bearer {self._api_key}"},
                    json={"input": batch, "model": self._model},
                )
                response.raise_for_status()
                data = response.json()["data"]
                data.sort(key=lambda x: x["index"])
                all_embeddings.extend(item["embedding"] for item in data)
        return all_embeddings


class OllamaEmbeddingGateway(EmbeddingGateway):
    def __init__(self) -> None:
        self._base_url = settings.ollama_base_url
        self._model = settings.embedding_model

    async def generate(self, texts: list[str]) -> list[list[float]]:
        embeddings: list[list[float]] = []
        async with httpx.AsyncClient(timeout=120.0) as client:
            for text in texts:
                response = await client.post(
                    f"{self._base_url}/api/embeddings",
                    json={"model": self._model, "prompt": text},
                )
                response.raise_for_status()
                embeddings.append(response.json()["embedding"])
        return embeddings


def create_embedding_gateway() -> EmbeddingGateway:
    provider = settings.embedding_provider.lower()
    if provider == "openai":
        return OpenAIEmbeddingGateway()
    if provider == "ollama":
        return OllamaEmbeddingGateway()
    if provider in ("local", "stub"):
        return LocalEmbeddingGateway(dimension=settings.embedding_dimension)
    return LocalEmbeddingGateway(dimension=settings.embedding_dimension)
