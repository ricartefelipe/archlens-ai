#!/usr/bin/env bash
# Emite certificado Let's Encrypt na EC2 piloto e ativa HTTPS no nginx.
#
# Uso:
#   source .aws-deploy/last-ec2.env
#   ./scripts/aws-setup-dns-auto.sh && CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ARCHLENS_DOMAIN="${ARCHLENS_DOMAIN:-}"
ARCHLENS_HOST="${ARCHLENS_HOST:-}"
ARCHLENS_KEY="${ARCHLENS_KEY:-$HOME/.ssh/archlens-deploy.pem}"
ARCHLENS_USER="${ARCHLENS_USER:-ec2-user}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/archlens/archlens-ai}"
ENV_FILE="${ENV_FILE:-$ROOT/.env.aws-pilot}"

if [[ -z "$ARCHLENS_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  ARCHLENS_HOST="${PUBLIC_IP:-}"
  ARCHLENS_KEY="${KEY_FILE:-$ARCHLENS_KEY}"
  ARCHLENS_DOMAIN="${ARCHLENS_DOMAIN:-${PILOT_DOMAIN:-}}"
fi

if [[ -z "$ARCHLENS_DOMAIN" && -n "$ARCHLENS_HOST" ]]; then
  ARCHLENS_DOMAIN="${ARCHLENS_HOST//./-}.sslip.io"
fi

if [[ -z "$ARCHLENS_HOST" || -z "$ARCHLENS_DOMAIN" ]]; then
  echo "Defina ARCHLENS_HOST / .aws-deploy/last-ec2.env" >&2
  exit 1
fi

SSH=(ssh -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new "${ARCHLENS_USER}@${ARCHLENS_HOST}")

echo "▸ Verificando DNS: $ARCHLENS_DOMAIN → $ARCHLENS_HOST"
RESOLVED="$(dig +short "$ARCHLENS_DOMAIN" A 2>/dev/null | head -1 || true)"
if [[ "$RESOLVED" != "$ARCHLENS_HOST" ]]; then
  echo "DNS não aponta para a EC2. Rode aws-setup-dns-auto.sh" >&2
  exit 1
fi

RSYNC_SSH="ssh -i $ARCHLENS_KEY -o StrictHostKeyChecking=accept-new"
rsync -az -e "$RSYNC_SSH" \
  "$ROOT/docker-compose.prod.tls.yml" \
  "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/"

"${SSH[@]}" "mkdir -p ${DEPLOY_DIR}/scripts"
rsync -az -e "$RSYNC_SSH" \
  "$ROOT/scripts/aws-setup-tls-ec2-remote.sh" \
  "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/scripts/"

rsync -az -e "$RSYNC_SSH" \
  "$ROOT/deploy/nginx/conf.d/" \
  "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/deploy/nginx/conf.d/"

HTTPS_BASE="https://${ARCHLENS_DOMAIN}"
if [[ -f "$ENV_FILE" ]]; then
  sed -i "s|^DOMAIN=.*|DOMAIN=${ARCHLENS_DOMAIN}|" "$ENV_FILE"
  sed -i "s|^APP_URL=.*|APP_URL=${HTTPS_BASE}|" "$ENV_FILE"
  sed -i "s|^KEYCLOAK_PUBLIC_URL=.*|KEYCLOAK_PUBLIC_URL=${HTTPS_BASE}/auth|" "$ENV_FILE"
  sed -i "s|^KEYCLOAK_ISSUER_URL=.*|KEYCLOAK_ISSUER_URL=${HTTPS_BASE}/auth/realms/archlens|" "$ENV_FILE"
  "$ROOT/scripts/generate-keycloak-realm-prod.sh" "$HTTPS_BASE"
  scp -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new \
    "$ENV_FILE" "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/.env.aws-pilot"
  scp -i "$ARCHLENS_KEY" -o StrictHostKeyChecking=accept-new \
    "$ROOT/infra/keycloak/realm-prod.json" \
    "${ARCHLENS_USER}@${ARCHLENS_HOST}:${DEPLOY_DIR}/infra/keycloak/realm-prod.json"
fi

"${SSH[@]}" bash <<REMOTE
set -euo pipefail
cd ${DEPLOY_DIR}
export ARCHLENS_DOMAIN=${ARCHLENS_DOMAIN}
export CERTBOT_EMAIL=${CERTBOT_EMAIL}
bash scripts/aws-setup-tls-ec2-remote.sh
REMOTE

echo ""
echo "✔ HTTPS ativo: ${HTTPS_BASE}/"
echo "  Rebuild frontend recomendado após mudança de APP_URL:"
echo "  ./scripts/aws-deploy-archlens-ec2.sh"
