"""Seleção de arquivos para ingestão: contenção de caminho e exclusão de credenciais.

Espelha `FileClassifier.isSensitive` do backend. A duplicação é intencional: o
worker precisa se defender por conta própria, pois recebe a lista de caminhos
pela API e não pode presumir que o chamador já filtrou.
"""

from pathlib import Path

SENSITIVE_SUFFIXES = (
    ".pem",
    ".key",
    ".p12",
    ".pfx",
    ".jks",
    ".keystore",
    ".truststore",
    ".ppk",
    ".asc",
    ".gpg",
    ".kdbx",
    ".tfvars",
    ".tfstate",
    ".pub",
)

SENSITIVE_NAMES = frozenset(
    {
        ".npmrc",
        ".pypirc",
        ".netrc",
        "_netrc",
        ".htpasswd",
        ".pgpass",
        "terraform.tfstate.backup",
        "credentials",
        "kubeconfig",
    }
)

SENSITIVE_NAME_PREFIXES = (".env", "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519", "secret.")

SENSITIVE_PATH_FRAGMENTS = (
    ".ssh/",
    ".gnupg/",
    ".aws/credentials",
    ".kube/config",
    ".docker/config.json",
)


def is_sensitive(relative_path: str) -> bool:
    """Arquivo cuja razão de existir é guardar credencial: nunca indexar."""
    lower = relative_path.replace("\\", "/").lower()
    name = lower.rsplit("/", 1)[-1]

    if any(fragment in lower for fragment in SENSITIVE_PATH_FRAGMENTS):
        return True
    if name.endswith(SENSITIVE_SUFFIXES):
        return True
    if name in SENSITIVE_NAMES or name.startswith(SENSITIVE_NAME_PREFIXES):
        return True
    return (
        "secrets." in name
        or "credentials." in name
        or "-secret." in name
        or "_secret." in name
    )


def resolve_within(base: Path, relative_path: str) -> Path | None:
    """Resolve `relative_path` sob `base`, ou None se escapar do diretório.

    Impede que uma lista de caminhos recebida pela API leia arquivos de outro
    projeto/tenant por travessia (`../`) ou caminho absoluto.
    """
    candidate = Path(relative_path)
    if candidate.is_absolute():
        return None

    base_resolved = base.resolve()
    try:
        resolved = (base_resolved / candidate).resolve()
    except (OSError, RuntimeError):
        return None

    if resolved == base_resolved or base_resolved not in resolved.parents:
        return None
    return resolved


def selectable_paths(base: Path, file_paths: list[str] | None) -> list[Path]:
    """Caminhos a indexar: contidos em `base` e não sensíveis.

    Sem `file_paths`, varre o diretório do projeto. Em ambos os casos o filtro
    de arquivos sensíveis é aplicado.
    """
    base_resolved = base.resolve()

    if file_paths:
        candidates: list[Path] = []
        for relative in file_paths:
            resolved = resolve_within(base, relative)
            if resolved is None or not resolved.is_file():
                continue
            candidates.append(resolved)
    else:
        candidates = [p for p in base_resolved.rglob("*") if p.is_file()]

    selected: list[Path] = []
    for path in candidates:
        try:
            relative = str(path.relative_to(base_resolved))
        except ValueError:
            continue
        if is_sensitive(relative):
            continue
        selected.append(path)
    return selected
