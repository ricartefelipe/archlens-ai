import asyncio

import pytest

from app.embedding import startup_verify
from app.embedding.startup_verify import verify_embedding_dimension_on_startup


class _FakeGateway:
    def __init__(self, vector):
        self._vector = vector

    async def generate(self, texts):
        return [self._vector]


def _run():
    asyncio.run(verify_embedding_dimension_on_startup())


def test_skips_when_verification_disabled(monkeypatch):
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension_verify", False)

    called = {"value": False}

    def _factory():
        called["value"] = True
        return _FakeGateway([0.0])

    monkeypatch.setattr(startup_verify, "create_embedding_gateway", _factory)
    _run()
    assert called["value"] is False


def test_passes_when_dimension_matches(monkeypatch):
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension_verify", True)
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension", 4)
    monkeypatch.setattr(startup_verify, "create_embedding_gateway", lambda: _FakeGateway([0.1, 0.2, 0.3, 0.4]))

    _run()  # não deve levantar


def test_raises_on_dimension_mismatch(monkeypatch):
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension_verify", True)
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension", 4)
    monkeypatch.setattr(startup_verify, "create_embedding_gateway", lambda: _FakeGateway([0.1, 0.2, 0.3]))

    with pytest.raises(RuntimeError):
        _run()


def test_raises_on_empty_vector(monkeypatch):
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension_verify", True)
    monkeypatch.setattr(startup_verify.settings, "embedding_dimension", 4)
    monkeypatch.setattr(startup_verify, "create_embedding_gateway", lambda: _FakeGateway([]))

    with pytest.raises(RuntimeError):
        _run()
