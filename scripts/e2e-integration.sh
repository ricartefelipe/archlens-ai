#!/usr/bin/env bash
# E2E integrado: stack local + seed demo-loja + Playwright contra APP_URL.
#
# Pré-requisitos: docker compose, Node 22, sample em samples/demo-loja
#
# Uso:
#   ./scripts/e2e-integration.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$ROOT/docker-compose.yml"

cleanup() {
  if [[ -n "${COMPOSE_PID:-}" ]]; then
    kill "$COMPOSE_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "▸ Subindo stack (postgres, rabbitmq, backend, worker, frontend)..."
docker compose -f "$COMPOSE_FILE" up -d --build postgres rabbitmq keycloak worker-ai backend frontend 2>&1 | tail -20

echo "▸ Aguardando health backend..."
for _ in $(seq 1 60); do
  if curl -sf http://localhost:8080/q/health/ready >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

echo "▸ Seed demo-loja"
BASE=http://localhost:8080 ARCHLENS_EMAIL=architect@archlens.dev ARCHLENS_PASSWORD=arch123 \
  "$ROOT/scripts/seed-demo-loja.sh"

echo "▸ Playwright E2E (frontend dev com API real via proxy manual)"
cd "$ROOT/e2e"
npm ci
npx playwright install chromium --with-deps
E2E_BASE_URL=http://localhost:3000 E2E_SKIP_SERVER=1 npm test

echo "✔ E2E integrado concluído"
