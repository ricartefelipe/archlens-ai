#!/usr/bin/env bash
# Recria o banco Keycloak e reimporta realm-prod.json (novos clients, ex.: archlens-bff).
#
# Uso na EC2:
#   cd /opt/archlens/archlens-ai && sudo bash scripts/aws-reimport-keycloak-realm.sh
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/archlens/archlens-ai}"
cd "$DEPLOY_DIR"

COMPOSE="sudo docker-compose"
CF="--env-file .env.aws-pilot -f docker-compose.prod.yml -f docker-compose.prod.tls.yml"

echo "▸ Parando Keycloak"
$COMPOSE $CF stop keycloak

echo "▸ Recriando database keycloak"
$COMPOSE $CF exec -T postgres psql -U archlens -d postgres <<'SQL'
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'keycloak' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS keycloak;
CREATE DATABASE keycloak OWNER archlens;
SQL

echo "▸ Subindo Keycloak (import realm)"
$COMPOSE $CF up -d keycloak
sleep 25

echo "▸ Recriando backend (OIDC discovery)"
$COMPOSE $CF up -d --force-recreate backend
sleep 15

echo "✔ Realm Keycloak reimportado"
