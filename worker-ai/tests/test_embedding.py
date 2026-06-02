import asyncio

from app.embedding import gateway as gateway_module
from app.embedding.gateway import (
    LocalEmbeddingGateway,
    OllamaEmbeddingGateway,
    OpenAIEmbeddingGateway,
    create_embedding_gateway,
)


def test_local_embedding_is_deterministic_and_normalized():
    gw = LocalEmbeddingGateway(dimension=16)
    vectors = asyncio.run(gw.generate(["arquitetura", "arquitetura"]))

    assert len(vectors) == 2
    assert len(vectors[0]) == 16
    assert vectors[0] == vectors[1]
    norm = sum(x * x for x in vectors[0]) ** 0.5
    assert abs(norm - 1.0) < 1e-9


def test_local_embedding_differs_per_text():
    gw = LocalEmbeddingGateway(dimension=16)
    vectors = asyncio.run(gw.generate(["alpha", "beta"]))
    assert vectors[0] != vectors[1]


def test_factory_selects_provider(monkeypatch):
    monkeypatch.setattr(gateway_module.settings, "embedding_provider", "local")
    assert isinstance(create_embedding_gateway(), LocalEmbeddingGateway)

    monkeypatch.setattr(gateway_module.settings, "embedding_provider", "desconhecido")
    assert isinstance(create_embedding_gateway(), LocalEmbeddingGateway)

    monkeypatch.setattr(gateway_module.settings, "embedding_provider", "openai")
    assert isinstance(create_embedding_gateway(), OpenAIEmbeddingGateway)

    monkeypatch.setattr(gateway_module.settings, "embedding_provider", "ollama")
    assert isinstance(create_embedding_gateway(), OllamaEmbeddingGateway)
