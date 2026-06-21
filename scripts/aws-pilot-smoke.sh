#!/usr/bin/env bash
# Smoke piloto AWS — health + login page.
#
# Uso:
#   source .aws-deploy/last-ec2.env
#   ./scripts/aws-pilot-smoke.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
fi

BASE="${APP_URL:-}"
if [[ -z "$BASE" && -n "${PILOT_DOMAIN:-}" ]]; then
  if [[ -f "$ROOT/.env.aws-pilot" ]] && grep -q '^APP_URL=https://' "$ROOT/.env.aws-pilot"; then
    BASE="$(grep '^APP_URL=' "$ROOT/.env.aws-pilot" | cut -d= -f2-)"
  else
    BASE="http://${PILOT_DOMAIN}"
  fi
fi

if [[ -z "$BASE" ]]; then
  echo "Defina APP_URL ou rode após deploy (.aws-deploy/last-ec2.env)." >&2
  exit 1
fi

echo "▸ Smoke em $BASE"

curl -sf "${BASE}/health" | head -c 200
echo ""
curl -sf -o /dev/null -w "login HTTP %{http_code}\n" "${BASE}/login"
curl -sf -o /dev/null -w "keycloak HTTP %{http_code}\n" "${BASE}/auth/realms/archlens" || true

echo "✔ Smoke básico concluído"
