#!/usr/bin/env bash
# Reseta contadores comerciais do tenant demo (análises/upload do período).
#
# Uso local (stack docker compose na máquina):
#   ./scripts/reset-demo-tenant.sh
#
# Uso remoto (EC2 do piloto):
#   source .aws-deploy/last-ec2.env
#   ./scripts/reset-demo-tenant.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TENANT="${ARCHLENS_TENANT:-tenant-1}"

ARCHLENS_HOST="${ARCHLENS_HOST:-}"
ARCHLENS_KEY="${ARCHLENS_KEY:-$HOME/.ssh/archlens-deploy.pem}"
ARCHLENS_USER="${ARCHLENS_USER:-ec2-user}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/archlens/archlens-ai}"

if [[ -z "$ARCHLENS_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  ARCHLENS_HOST="${PUBLIC_IP:-}"
  ARCHLENS_KEY="${KEY_FILE:-$ARCHLENS_KEY}"
fi

run_psql() {
  local sql=$1
  if [[ -n "$ARCHLENS_HOST" ]]; then
    ssh -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new "${ARCHLENS_USER}@${ARCHLENS_HOST}" bash -s <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
POSTGRES_PASSWORD=\$(grep '^POSTGRES_PASSWORD=' .env.aws-pilot | cut -d= -f2-)
sudo docker compose --env-file .env.aws-pilot -f docker-compose.prod.yml exec -T postgres \
  psql -U archlens -d archlens -c "${sql}"
REMOTE
  elif command -v docker >/dev/null && docker compose -f "$ROOT/docker-compose.yml" ps postgres 2>/dev/null | grep -q Up; then
    docker compose -f "$ROOT/docker-compose.yml" exec -T postgres \
      psql -U archlens -d archlens -c "$sql"
  else
    echo "Nenhum postgres acessível. Defina ARCHLENS_HOST ou suba docker compose local." >&2
    exit 1
  fi
}

SQL="UPDATE tenant_accounts SET analyses_used_period = 0, upload_bytes_period = 0, updated_at = NOW() WHERE tenant_id = '${TENANT}';"

echo "▸ Resetando uso comercial de ${TENANT}…"
run_psql "$SQL"
echo "✔ Contadores zerados para ${TENANT}"
