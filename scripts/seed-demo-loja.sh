#!/usr/bin/env bash
# Cria projeto demo-loja no piloto: zip sample → upload → análise.
#
# Uso:
#   ./scripts/seed-demo-loja.sh
#   BASE=http://56-124-121-17.sslip.io ./scripts/seed-demo-loja.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLE_DIR="$ROOT/samples/demo-loja"
ZIP_FILE="/tmp/demo-loja-$$.zip"

BASE="${BASE:-}"
if [[ -z "$BASE" && -f "$ROOT/.env.aws-pilot" ]]; then
  BASE="$(grep '^APP_URL=' "$ROOT/.env.aws-pilot" | cut -d= -f2-)"
fi
if [[ -z "$BASE" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  BASE="http://${PILOT_DOMAIN:-$PUBLIC_IP}"
fi
BASE="${BASE:-http://56-124-121-17.sslip.io}"
if [[ "$BASE" == https://* ]]; then
  BASE="http://${BASE#https://}"
fi

EMAIL="${ARCHLENS_EMAIL:-architect@archlens.dev}"
PASSWORD="${ARCHLENS_PASSWORD:-arch123}"
ADMIN_EMAIL="${ARCHLENS_ADMIN_EMAIL:-admin@archlens.dev}"
ADMIN_PASSWORD="${ARCHLENS_ADMIN_PASSWORD:-admin123}"
TENANT="${ARCHLENS_TENANT:-tenant-1}"
PROJECT_NAME="${PROJECT_NAME:-demo-loja}"

cleanup() { rm -f "$ZIP_FILE"; }
trap cleanup EXIT

if [[ ! -d "$SAMPLE_DIR" ]]; then
  echo "Sample não encontrado: $SAMPLE_DIR" >&2
  exit 1
fi

echo "▸ Empacotando sample"
(
  cd "$SAMPLE_DIR"
  zip -qr "$ZIP_FILE" .
)

login() {
  local user=$1 pass=$2
  curl -sf -X POST "$BASE/public/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$user\",\"password\":\"$pass\"}"
}

echo "▸ Login admin (libera quota se necessário)"
ADMIN_JSON=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
ADMIN_TOKEN=$(python3 -c "import json,sys; print(json.load(sys.stdin)['accessToken'])" <<<"$ADMIN_JSON")

USAGE=$(curl -sf "$BASE/v1/account/usage" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "X-Tenant-Id: $TENANT")
PROJECTS_USED=$(python3 -c "import json,sys; print(json.load(sys.stdin)['projectsUsed'])" <<<"$USAGE")
PROJECTS_LIMIT=$(python3 -c "import json,sys; print(json.load(sys.stdin)['projectsLimit'])" <<<"$USAGE")
PLAN=$(python3 -c "import json,sys; print(json.load(sys.stdin)['plan'])" <<<"$USAGE")

if [[ "$PLAN" == "PILOT" && "$PROJECTS_USED" -ge "$PROJECTS_LIMIT" ]]; then
  echo "▸ Plano PILOT com projeto cheio — upgrade para DIAGNOSTICO"
  curl -sf -X PUT "$BASE/v1/admin/tenants/$TENANT/plan" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "X-Tenant-Id: $TENANT" \
    -H 'Content-Type: application/json' \
    -d '{"plan":"DIAGNOSTICO","notes":"Demo demo-loja"}' >/dev/null
fi

echo "▸ Login architect"
USER_JSON=$(login "$EMAIL" "$PASSWORD")
TOKEN=$(python3 -c "import json,sys; print(json.load(sys.stdin)['accessToken'])" <<<"$USER_JSON")

echo "▸ Criando projeto $PROJECT_NAME"
PROJECT=$(curl -sf -X POST "$BASE/v1/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"$PROJECT_NAME\",\"description\":\"Sample ArchLens — loja fictícia\"}")
PROJECT_ID=$(python3 -c "import json,sys; print(json.load(sys.stdin)['id'])" <<<"$PROJECT")

echo "▸ Upload $(du -h "$ZIP_FILE" | cut -f1)"
curl -sf -X POST "$BASE/v1/projects/$PROJECT_ID/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT" \
  -F "file=@$ZIP_FILE;type=application/zip" >/dev/null

echo "▸ Aguardando ingestão (READY)"
for _ in $(seq 1 60); do
  STATUS=$(curl -sf "$BASE/v1/projects/$PROJECT_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT" | python3 -c "import json,sys; print(json.load(sys.stdin)['status'])")
  if [[ "$STATUS" == "READY" ]]; then
    break
  fi
  if [[ "$STATUS" == "FAILED" ]]; then
    echo "Ingestão falhou" >&2
    exit 1
  fi
  sleep 5
done

echo "▸ Iniciando análise"
ANALYSIS=$(curl -sf -X POST "$BASE/v1/projects/$PROJECT_ID/analyses" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT")
ANALYSIS_ID=$(python3 -c "import json,sys; print(json.load(sys.stdin)['id'])" <<<"$ANALYSIS")

echo "▸ Aguardando análise"
for _ in $(seq 1 90); do
  A=$(curl -sf "$BASE/v1/projects/$PROJECT_ID/analyses/$ANALYSIS_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT")
  STATUS=$(python3 -c "import json,sys; print(json.load(sys.stdin)['status'])" <<<"$A")
  if [[ "$STATUS" == "COMPLETED" ]]; then
    RISKS=$(python3 -c "import json,sys; print(len(json.load(sys.stdin).get('risks',[])))" <<<"$A")
    SUMMARY=$(python3 -c "import json,sys; print(json.load(sys.stdin).get('summary','')[:120])" <<<"$A")
    echo ""
    echo "✔ Demo pronto"
    echo "  Projeto:  $BASE/projects/$PROJECT_ID"
    echo "  Análise:  $BASE/projects/$PROJECT_ID/analyses/$ANALYSIS_ID"
    echo "  Chat:     $BASE/projects/$PROJECT_ID/chat?analysisId=$ANALYSIS_ID"
    echo "  Export:   $BASE/v1/projects/$PROJECT_ID/analyses/$ANALYSIS_ID/report?format=pdf"
    echo "  Riscos:   $RISKS — ${SUMMARY}"
    echo "  Login:    $EMAIL / $PASSWORD"
    exit 0
  fi
  if [[ "$STATUS" == "FAILED" ]]; then
    echo "Análise falhou" >&2
    exit 1
  fi
  sleep 10
done

echo "Análise ainda em andamento: $BASE/projects/$PROJECT_ID/analyses/$ANALYSIS_ID" >&2
exit 0
