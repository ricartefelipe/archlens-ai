#!/usr/bin/env bash
# DNS automático piloto EC2 — sslip.io (ex.: 54-94-52-89.sslip.io → IP).
#
# Uso:
#   ARCHLENS_HOST=54.94.52.89 ./scripts/aws-setup-dns-auto.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ARCHLENS_HOST="${ARCHLENS_HOST:-}"
if [[ -z "$ARCHLENS_HOST" && -f "$ROOT/.aws-deploy/last-ec2.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/.aws-deploy/last-ec2.env"
  ARCHLENS_HOST="${PUBLIC_IP:-}"
fi

if [[ -z "$ARCHLENS_HOST" ]]; then
  echo "Defina ARCHLENS_HOST (IP público da EC2)." >&2
  exit 1
fi

ARCHLENS_DOMAIN="${ARCHLENS_DOMAIN:-${ARCHLENS_HOST//./-}.sslip.io}"

echo "▸ Domínio piloto: $ARCHLENS_DOMAIN → $ARCHLENS_HOST"

for i in $(seq 1 15); do
  RESOLVED="$(dig +short "$ARCHLENS_DOMAIN" A 2>/dev/null | head -1 || true)"
  if [[ "$RESOLVED" == "$ARCHLENS_HOST" ]]; then
    echo "✔ DNS OK"
    echo "ARCHLENS_DOMAIN=$ARCHLENS_DOMAIN"
    exit 0
  fi
  [[ "$i" -eq 1 ]] && echo "▸ Aguardando DNS..."
  sleep 2
done

echo "DNS não resolveu (esperado $ARCHLENS_HOST, obtido ${RESOLVED:-vazio})." >&2
exit 1
