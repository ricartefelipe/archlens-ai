"""Contenção de caminho e exclusão de credenciais na seleção de arquivos."""

from pathlib import Path

import pytest

from app.service.file_filter import is_sensitive, resolve_within, selectable_paths


@pytest.mark.parametrize(
    "path",
    [
        "env/prod.tfvars",
        "terraform.tfstate",
        ".env",
        ".env.production",
        "config/credentials.json",
        "k8s/db-secret.yaml",
        "infra/secrets.yml",
        "certs/server.key",
        "certs/server.pem",
        "keystore.jks",
        "home/.ssh/id_rsa",
        ".npmrc",
    ],
)
def test_sensitive_files_are_detected(path):
    assert is_sensitive(path)


@pytest.mark.parametrize(
    "path",
    [
        "src/main/java/App.java",
        "src/main/resources/application.properties",
        "infra/main.tf",
        "docker-compose.yml",
        "k8s/deployment.yaml",
        "openapi.yaml",
    ],
)
def test_legitimate_configuration_is_kept(path):
    # Configuração legítima continua sendo analisada: é nela que a análise
    # aponta segredo embutido como risco.
    assert not is_sensitive(path)


def test_resolve_within_rejects_traversal_and_absolute(tmp_path):
    base = tmp_path / "project-a"
    base.mkdir()
    (base / "ok.java").write_text("class Ok {}")

    other = tmp_path / "project-b"
    other.mkdir()
    (other / "leak.java").write_text("class Leak {}")

    assert resolve_within(base, "ok.java") == (base / "ok.java").resolve()
    assert resolve_within(base, "../project-b/leak.java") is None
    assert resolve_within(base, "/etc/passwd") is None
    assert resolve_within(base, "..") is None


def test_selectable_paths_filters_traversal_and_secrets(tmp_path):
    base = tmp_path / "project-a"
    (base / "src").mkdir(parents=True)
    (base / "env").mkdir()
    (base / "src" / "App.java").write_text("class App {}")
    (base / "env" / "prod.tfvars").write_text('db_password = "s3cr3t"')
    (base / ".env").write_text("API_KEY=abc")

    other = tmp_path / "project-b"
    other.mkdir()
    (other / "leak.java").write_text("class Leak {}")

    selected = selectable_paths(
        base,
        [
            "src/App.java",
            "env/prod.tfvars",
            ".env",
            "../project-b/leak.java",
            "nao-existe.java",
        ],
    )

    assert [p.name for p in selected] == ["App.java"]


def test_selectable_paths_without_list_scans_and_filters(tmp_path):
    base = tmp_path / "project-a"
    (base / "src").mkdir(parents=True)
    (base / "src" / "App.java").write_text("class App {}")
    (base / "secrets.yml").write_text("pw: x")

    selected = sorted(p.name for p in selectable_paths(base, None))

    assert selected == ["App.java"]


def test_selectable_paths_ignores_symlink_escaping_base(tmp_path):
    base = tmp_path / "project-a"
    base.mkdir()
    outside = tmp_path / "outside.java"
    outside.write_text("class Outside {}")
    (base / "link.java").symlink_to(outside)

    # O symlink resolve para fora da base: não deve ser selecionado.
    assert selectable_paths(base, ["link.java"]) == []
