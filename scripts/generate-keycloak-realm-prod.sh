#!/usr/bin/env bash
# Gera realm Keycloak de produção/piloto a partir do template.
#
# Uso:
#   ./scripts/generate-keycloak-realm-prod.sh https://54-94-52-89.sslip.io
#   APP_URL=https://app.seudominio.com.br ./scripts/generate-keycloak-realm-prod.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEMPLATE="$ROOT/infra/keycloak/realm-prod-template.json"
OUTPUT="$ROOT/infra/keycloak/realm-prod.json"

APP_URL="${1:-${APP_URL:-}}"
if [[ -z "$APP_URL" ]]; then
  echo "Uso: $0 https://seu-dominio" >&2
  echo "  ou APP_URL=https://seu-dominio $0" >&2
  exit 1
fi

APP_URL="${APP_URL%/}"

if [[ ! -f "$TEMPLATE" ]]; then
  echo "Template não encontrado: $TEMPLATE" >&2
  exit 1
fi

sed "s|\${APP_URL}|${APP_URL}|g" "$TEMPLATE" > "$OUTPUT"
echo "Realm gerado: $OUTPUT (APP_URL=$APP_URL)"
