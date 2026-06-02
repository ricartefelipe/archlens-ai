from uuid import uuid4

from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


def test_health_endpoint():
    with TestClient(app) as client:
        response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_analyze_endpoint_returns_findings(monkeypatch, tmp_path):
    project_id = uuid4()
    project_dir = tmp_path / str(project_id)
    project_dir.mkdir()
    (project_dir / "Dockerfile").write_text("FROM python:latest\n", encoding="utf-8")

    monkeypatch.setattr(settings, "storage_base_path", str(tmp_path))

    with TestClient(app) as client:
        response = client.post(
            f"/v1/analyze/{project_id}",
            json={"project_id": str(project_id), "tenant_id": "tenant-1"},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["total_files_analyzed"] == 1
    assert len(body["findings"]) >= 1


def test_analyze_endpoint_rejects_project_id_mismatch():
    with TestClient(app) as client:
        response = client.post(
            f"/v1/analyze/{uuid4()}",
            json={"project_id": str(uuid4()), "tenant_id": "tenant-1"},
        )
    assert response.status_code == 400
