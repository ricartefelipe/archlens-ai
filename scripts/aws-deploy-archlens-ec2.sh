#!/usr/bin/env bash
# Deploy ArchLens AI na EC2 via SSH (rsync + docker compose prod + Keycloak OIDC).
#
# Uso:
#   ARCHLENS_HOST=1.2.3.4 ARCHLENS_KEY=~/.ssh/archlens-deploy.pem ./scripts/aws-deploy-archlens-ec2.sh
#   source .aws-deploy/last-ec2.env && ./scripts/aws-deploy-archlens-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ARCHLENS_HOST="${ARCHLENS_HOST:-}"
ARCHLENS_KEY="${ARCHLENS_KEY:-$HOME/.ssh/archlens-deploy.pem}"
ARCHLENS_USER="${ARCHLENS_USER:-ec2-user}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/archlens/archlens-ai}"

if [[ -z "$ARCHLENS_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  ARCHLENS_HOST="${PUBLIC_IP:-$ARCHLENS_HOST}"
  ARCHLENS_KEY="${KEY_FILE:-$ARCHLENS_KEY}"
  ARCHLENS_DOMAIN="${PILOT_DOMAIN:-$ARCHLENS_DOMAIN}"
fi

if [[ -z "$ARCHLENS_HOST" ]]; then
  echo "Defina ARCHLENS_HOST (IP público da EC2)." >&2
  exit 1
fi

ARCHLENS_DOMAIN="${ARCHLENS_DOMAIN:-${ARCHLENS_HOST//./-}.sslip.io}"
SCHEME="${SCHEME:-http}"
BASE_URL="${SCHEME}://${ARCHLENS_DOMAIN}"
KEYCLOAK_PUBLIC="${BASE_URL}/auth"
KEYCLOAK_ISSUER="${KEYCLOAK_PUBLIC}/realms/archlens"

SSH=(ssh -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new "${ARCHLENS_USER}@${ARCHLENS_HOST}")

echo "▸ Aguardando SSH em $ARCHLENS_HOST..."
for _ in $(seq 1 30); do
  if "${SSH[@]}" "test -f /opt/archlens/bootstrap.ok || command -v docker >/dev/null" 2>/dev/null; then
    break
  fi
  sleep 10
done

"${SSH[@]}" "docker --version || sudo docker --version"

ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "▸ Gerando $ENV_FILE (piloto — revisar secrets)"
  POSTGRES_PASSWORD="$(openssl rand -hex 16)"
  RABBITMQ_PASSWORD="$(openssl rand -hex 16)"
  KEYCLOAK_ADMIN_PASSWORD="$(openssl rand -hex 12)"
  KEYCLOAK_BFF_CLIENT_SECRET="$(openssl rand -hex 24)"
  cat > "$ENV_FILE" <<EOF
DOMAIN=${ARCHLENS_DOMAIN}
APP_URL=${BASE_URL}
KEYCLOAK_PUBLIC_URL=${KEYCLOAK_PUBLIC}
KEYCLOAK_ISSUER_URL=${KEYCLOAK_ISSUER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
RABBITMQ_USERNAME=archlens
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
KEYCLOAK_BFF_CLIENT_SECRET=${KEYCLOAK_BFF_CLIENT_SECRET}
EMBEDDING_PROVIDER=local
ARCHLENS_LLM_PROVIDER=local
OPENAI_API_KEY=
ARCHLENS_REPORT_BRAND_NAME=ArchLens AI
NEXT_PUBLIC_APP_NAME=ArchLens AI
NEXT_PUBLIC_PRIMARY_COLOR=#2563eb
NEXT_PUBLIC_SUPPORT_URL=mailto:consultoria@exemplo.com
EOF
  chmod 600 "$ENV_FILE"
else
  if ! grep -q '^KEYCLOAK_BFF_CLIENT_SECRET=' "$ENV_FILE"; then
    KEYCLOAK_BFF_CLIENT_SECRET="$(openssl rand -hex 24)"
    echo "KEYCLOAK_BFF_CLIENT_SECRET=${KEYCLOAK_BFF_CLIENT_SECRET}" >> "$ENV_FILE"
  fi
  KEYCLOAK_BFF_CLIENT_SECRET="$(grep '^KEYCLOAK_BFF_CLIENT_SECRET=' "$ENV_FILE" | cut -d= -f2-)"
  export KEYCLOAK_BFF_CLIENT_SECRET
  if ! grep -q '^ARCHLENS_ENFORCE_QUOTAS=' "$ENV_FILE"; then
    echo "ARCHLENS_ENFORCE_QUOTAS=false" >> "$ENV_FILE"
  fi
fi

echo "▸ Gerando realm Keycloak para $BASE_URL"
export KEYCLOAK_BFF_CLIENT_SECRET="${KEYCLOAK_BFF_CLIENT_SECRET:-$(grep '^KEYCLOAK_BFF_CLIENT_SECRET=' "$ENV_FILE" | cut -d= -f2-)}"
"$ROOT/scripts/generate-keycloak-realm-prod.sh" "$BASE_URL"

echo "▸ Sincronizando código na EC2..."
"${SSH[@]}" "sudo mkdir -p /opt/archlens && sudo chown ${ARCHLENS_USER}:${ARCHLENS_USER} /opt/archlens"

RSYNC_SSH="ssh -i $ARCHLENS_KEY -o StrictHostKeyChecking=accept-new"
rsync -az --delete \
  -e "$RSYNC_SSH" \
  --exclude '.git' --exclude 'node_modules' --exclude 'target' \
  --exclude '.env' --exclude '.env.aws-pilot' --exclude '.aws-deploy' \
  --exclude 'frontend/.next' --exclude 'frontend/node_modules' \
  --exclude 'worker-ai/.venv' --exclude 'worker-ai/__pycache__' \
  --exclude 'e2e/test-results' \
  "$ROOT/" "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/"

scp -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new \
  "$ENV_FILE" "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/.env.aws-pilot"

echo "▸ Build + compose up (~20–35 min na t3.large)..."
"${SSH[@]}" bash <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
sudo usermod -aG docker ${ARCHLENS_USER} 2>/dev/null || true
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
fi
cp -f deploy/nginx/conf.d/default-pilot.conf deploy/nginx/conf.d/default-pilot-active.conf
COMPOSE="\$(command -v docker-compose || echo docker-compose)"
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml build 2>&1 | tail -25
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml up -d
sleep 60
sudo \$COMPOSE --env-file .env.aws-pilot -f docker-compose.prod.yml ps
curl -sf http://127.0.0.1/health && echo " nginx/backend OK" || echo " aguardando health..."
REMOTE

echo ""
echo "✔ Deploy ArchLens AI"
echo "  App:      ${BASE_URL}/"
echo "  API:      ${BASE_URL}/v1/projects"
echo "  Health:   ${BASE_URL}/health"
echo "  Keycloak: ${KEYCLOAK_PUBLIC}/"
echo "  Admin KC: ${KEYCLOAK_PUBLIC}/admin (user admin, senha em .env.aws-pilot)"
echo ""
echo "Login demo (realm importado):"
echo "  admin@archlens.dev / admin123  (platform admin + tenant-1)"
echo "  architect@archlens.dev / arch123"
echo ""
echo "TLS (recomendado):"
echo "  source .aws-deploy/last-ec2.env"
echo "  CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh"
