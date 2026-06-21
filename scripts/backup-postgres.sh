#!/usr/bin/env bash
# Backup Postgres na EC2 (archlens + keycloak).
#
# Uso na EC2:
#   cd /opt/archlens/archlens-ai && ./scripts/backup-postgres.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/archlens/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$BACKUP_DIR"

COMPOSE="docker-compose"
if ! command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="sudo docker compose"
fi

cd "$ROOT"
ENV_FILE="${ENV_FILE:-.env.aws-pilot}"

$COMPOSE --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U archlens -d archlens | gzip > "${BACKUP_DIR}/archlens-${STAMP}.sql.gz"

$COMPOSE --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U archlens -d keycloak | gzip > "${BACKUP_DIR}/keycloak-${STAMP}.sql.gz"

echo "✔ Backup em ${BACKUP_DIR}/archlens-${STAMP}.sql.gz"
